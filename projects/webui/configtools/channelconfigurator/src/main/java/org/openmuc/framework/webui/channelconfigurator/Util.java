/*
 * Copyright 2011-14 Fraunhofer ISE
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
package org.openmuc.framework.webui.channelconfigurator;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Frederic Robra
 */
public class Util {

    public static Double tryParseDouble(String input) {
        if (input == null || input.isEmpty() || input.equals("null")) {
            return null;
        }
        try {
            return Double.parseDouble(input);
        }
        catch (NumberFormatException e) {
            return null;
        }
    }

    public static Integer tryParseInt(String input) {
        if (input == null || input.isEmpty() || input.equals("null")) {
            return null;
        }
        try {
            return Integer.parseInt(input);
        }
        catch (NumberFormatException e) {
            return null;
        }
    }

    public static String parseString(HttpServletRequest request, String parameterName)
            throws ProcessRequestException {
        String result = request.getParameter(parameterName);

        if (result == null || result.isEmpty() || result.equals("null")) {
            System.out.println(result);
            throw new ProcessRequestException(parameterName + " not found in request");
        }

        return result;
    }

    public static String tryParseString(HttpServletRequest request, String parameterName) {
        String result = request.getParameter(parameterName);

        if (result == null || result.isEmpty() || result.equals("null")) {
            return null;
        }

        return result;
    }
}
