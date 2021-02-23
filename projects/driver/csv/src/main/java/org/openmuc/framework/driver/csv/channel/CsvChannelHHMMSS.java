/*
 * Copyright 2011-2021 Fraunhofer ISE
 *
 * This file is part of OpenMUC.
 * For more information visit http://www.openmuc.org
 *
 * OpenMUC is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenMUC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenMUC.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.openmuc.framework.driver.csv.channel;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

import org.openmuc.framework.driver.csv.exceptions.CsvException;

public class CsvChannelHHMMSS extends CsvTimeChannel {

    public CsvChannelHHMMSS(List<String> data, boolean rewind, long[] timestamps) {
        super(data, rewind, timestamps);
    }

    @Override
    public String readValue(long samplingTime) throws CsvException {
        int hhmmss = convertTimestamp(samplingTime);
        lastReadIndex = searchNextIndex(hhmmss);
        return data.get(lastReadIndex);
    }

    private int convertTimestamp(long samplingTime) {

        GregorianCalendar cal = new GregorianCalendar(Locale.getDefault());
        cal.setTime(new Date(samplingTime));

        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);
        int second = cal.get(Calendar.SECOND);

        // convert sampling time (unixtimestamp) to sampling time (hhmmss)
        // 14:25:34
        // 140000 + 2500 + 34 = 142534
        int hhmmss = hour * 10000 + minute * 100 + second;
        return hhmmss;
    }

}
