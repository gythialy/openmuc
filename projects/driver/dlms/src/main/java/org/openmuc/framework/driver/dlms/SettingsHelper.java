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
package org.openmuc.framework.driver.dlms;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

public class SettingsHelper {

    private static final String SEND_DISCONNECT_KEY = "senddisconnect";
    private static final boolean SEND_DISCONNECT_DEFAULT = true;

    private static final String REFERENCING_KEY = "referencing";
    private static final String REFERENCING_DEFAULT = "LN";

    private static final String USE_HANDSHAKE_KEY = "usehandshake";
    private static final boolean USE_HANDSHAKE_DEFAULT = true;

    private static final String BAUDRATE_KEY = "baudrate";
    private static final int BAUDRATE_DEFAULT = 0;

    private static final String PASSWORD_KEY = "pw";
    private static final byte[] PASSWORD_DEFAULT = null;

    private static final String FORCE_SINGLE_KEY = "forcesingle";
    private static final boolean FORCE_SINGLE_DEFAULT = false;

    private static final Charset US_ASCII = Charset.forName("US-ASCII");

    private final Map<String, String> settingsMap = new HashMap<>();

    public SettingsHelper(String settings) {
        String[] settingsArray = settings.split(";");
        for (String arg : settingsArray) {
            int p = arg.indexOf("=");
            if (p != -1) {
                settingsMap.put(arg.substring(0, p).toLowerCase().trim(), arg.substring(p + 1).trim());
            }
            // ignore params with wrong formatting
            // TODO Add logging for wrongly formatted setting
        }
    }

    public boolean sendDisconnect() {
        if (settingsMap.containsKey(SEND_DISCONNECT_KEY)) {
            return Boolean.parseBoolean(settingsMap.get(SEND_DISCONNECT_KEY).trim());
        }

        return SEND_DISCONNECT_DEFAULT;
    }

    public String getReferencing() {
        if (settingsMap.containsKey(REFERENCING_KEY)) {
            return settingsMap.get(REFERENCING_KEY).toUpperCase();
        }

        return REFERENCING_DEFAULT;
    }

    public boolean useHandshake() {
        if (settingsMap.containsKey(USE_HANDSHAKE_KEY)) {
            return Boolean.parseBoolean(settingsMap.get(USE_HANDSHAKE_KEY).trim());
        }

        return USE_HANDSHAKE_DEFAULT;
    }

    public int getBaudrate() {
        if (settingsMap.containsKey(BAUDRATE_KEY)) {
            return Integer.parseInt(settingsMap.get(BAUDRATE_KEY).trim());
        }

        return BAUDRATE_DEFAULT;
    }

    public byte[] getPassword() {
        if (settingsMap.containsKey(PASSWORD_KEY)) {
            return settingsMap.get(PASSWORD_KEY).getBytes(US_ASCII);
        }

        return PASSWORD_DEFAULT;
    }

    public boolean forceSingle() {
        if (settingsMap.containsKey(FORCE_SINGLE_KEY)) {
            return Boolean.parseBoolean(settingsMap.get(FORCE_SINGLE_KEY).trim());
        }

        return FORCE_SINGLE_DEFAULT;
    }
}
