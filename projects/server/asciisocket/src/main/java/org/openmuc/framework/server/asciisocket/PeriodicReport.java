/*
 * Copyright 2011-15 Fraunhofer ISE
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
package org.openmuc.framework.server.asciisocket;

import java.io.PrintWriter;

public class PeriodicReport extends Report {

    private final int interval;
    private long lastReportTime = 0;

    public PeriodicReport(int handle, int interval) {
        super(handle);
        this.interval = interval;
    }

    @Override
    public void emitReport(PrintWriter out) {
        long curTime = System.currentTimeMillis();

        if (curTime >= (lastReportTime + interval)) {
            lastReportTime = curTime;
            super.emitReport(out);
        }
    }
}
