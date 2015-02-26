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
package org.openmuc.framework.demo.restclient;

import org.openmuc.framework.demo.restclient.json.JsonObject;

import java.util.ArrayList;

public class JsonHelper {

    public static String StringValueToJson(String value) {
        JsonObject jso = new JsonObject();

        jso.writeStartObject();
        jso.writeObjectField("value");
        try {
            jso.writeObjectValue(Double.valueOf(value));
        } catch (NumberFormatException e) {
            System.out.println("No valid double as argument for option -w (--write)\nRequest will fail:");
        }

        jso.writeEndObject();

        return jso.getJsonText();
    }

    public static String ChannelValuesToJson(ArrayList<String> wParam) {
        JsonObject jso = new JsonObject();
        jso.writeStartObject();
        int i = 0;
        for (String param : wParam) {
            if ((i & 01) == 0) {
                jso.writeObjectField(param);

            } else {
                try {
                    jso.writeObjectValue(Double.valueOf(param));
                } catch (NumberFormatException e) {
                    System.out.println("No valid double as argument for option -w (--write)\nRequest will fail:");
                }
            }
            ++i;
        }

        jso.writeEndObject();

        return jso.getJsonText();
    }
}
