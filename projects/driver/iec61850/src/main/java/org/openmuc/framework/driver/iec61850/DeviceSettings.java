/*
 * Copyright 2011-2022 Fraunhofer ISE
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

package org.openmuc.framework.driver.iec61850;

import org.openmuc.framework.config.ArgumentSyntaxException;

public class DeviceSettings {
    private String authentication = null;
    private byte[] tSelLocal = new byte[] { 0, 0 };
    private byte[] tSelRemote = new byte[] { 0, 1 };

    public DeviceSettings(String settings) throws ArgumentSyntaxException {
        if (!settings.isEmpty()) {
            String[] args = settings.split("\\s+", 0);
            if (args.length > 6 || args.length < 4) {
                throw new ArgumentSyntaxException(
                        "Less than 4 or more than 6 arguments in the settings are not allowed.");
            }
            for (int i = 0; i < args.length; i += 2) {
                if (args[i].equals("-a")) {
                    if (args[i + 1].equals("-lt")) {
                        throw new ArgumentSyntaxException(
                                "No authentication parameter was specified after the -a parameter");
                    }
                    authentication = args[i + 1];
                }
                else if (args[i].equals("-lt")) {
                    if (i == (args.length - 1) || args[i + 1].startsWith("-")) {
                        this.tSelLocal = new byte[0];
                    }
                    else {
                        this.tSelLocal = new byte[args[i + 1].length()];
                        for (int j = 0; j < args[i + 1].length(); j++) {
                            tSelLocal[j] = (byte) args[i + 1].charAt(j);
                        }
                    }
                }
                else if (args[i].equals("-rt")) {
                    if (i == (args.length - 1) || args[i + 1].startsWith("-")) {
                        this.tSelRemote = new byte[0];
                    }
                    else {
                        this.tSelRemote = new byte[args[i + 1].length()];
                        for (int j = 0; j < args[i + 1].length(); j++) {
                            tSelRemote[j] = (byte) args[i + 1].charAt(j);
                        }
                    }
                }
                else {
                    throw new ArgumentSyntaxException("Unexpected argument: " + args[i]);
                }
            }
        }
    }

    public String getAuthentication() {
        return authentication;
    }

    public byte[] getTSelLocal() {
        return tSelLocal;
    }

    public byte[] getTSelRemote() {
        return tSelRemote;
    }
}
