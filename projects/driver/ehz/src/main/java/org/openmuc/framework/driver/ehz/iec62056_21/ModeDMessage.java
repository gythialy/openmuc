/*
 * Copyright 2011-16 Fraunhofer ISE
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

package org.openmuc.framework.driver.ehz.iec62056_21;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class ModeDMessage {

    private final byte[] frame;

    private String vendorID;
    private String identifier;
    private List<String> dataSets;

    public List<String> getDataSets() {
        return dataSets;
    }

    public ModeDMessage(byte[] frame) {
        this.frame = frame;
    }

    public String getVendorID() {
        return vendorID;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void parse() throws ParseException {
        int position = 0;
        try {
            /* Check for start sign */
            if (frame[0] != '/') {
                throw new ParseException("Invalid character", 0);
            }

            /* Check for valid vendor ID (only upper case letters) */
            for (position = 1; position < 4; position++) {
                if (!(frame[position] > 64 && frame[position] < 91)) {
                    throw new ParseException("Invalid character", position);
                }
            }

            vendorID = new String(frame, 1, 3);

            /* Baud rate sign needs to be '0' .. '6' */
            if (frame[4] <= '0' || frame[4] >= '6') {
                throw new ParseException("Invalid character", 4);
            }

            position = 5;
            int i = 0;
            /* Search for CRLF to extract identifier */
            while (!((frame[position + i] == 0x0d) && (frame[position + i + 1] == 0x0a))) {
                if (frame[position + i] == '!') {
                    throw new ParseException("Invalid end character", position + i);
                }
                i++;
            }

            identifier = new String(frame, 5, i - 1);

            position += i;

            /* Skip next CRLF */
            position += 4;

            /* Get data sets */
            dataSets = new ArrayList<>();

            while (frame[position] != '!') {

                i = 0;

                while (frame[position + i] != 0x0d) {
                    i++;
                }

                String dataSet = new String(frame, position, i);

                dataSets.add(dataSet);

                position += (i + 2);

            }

        } catch (IndexOutOfBoundsException e) {
            throw new ParseException("Unexpected end of message", position);
        }

    }

}
