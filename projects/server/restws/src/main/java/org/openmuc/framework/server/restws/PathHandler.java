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
package org.openmuc.framework.server.restws;

public class PathHandler {

    public static Boolean isValidRequest(String pathInfo, String queryStr) {
        String regex = "^(\\/)([a-zA-Z0-9._-]*\\w)(\\/{0,1})";

        if (pathInfo.matches(regex) && queryStr == "") {
            return true;
        } else if (pathInfo.equals("/") && queryStr == "") {
            return true;
        } else {
            return false;
        }
    }

    public static Boolean isValidHistoryRequest(String pathInfo, String queryStr) {
        String regexPathInfo = "^(\\/)([a-zA-Z0-9._-]*\\w)(\\/)history";
        String regexQueryStr = "from=(\\d{1,13})&until=(\\d{1,20})";
        if (pathInfo.matches(regexPathInfo) && queryStr.matches(regexQueryStr)) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean isValidConfigRequest(String pathInfo, String queryStr) {
        String regex = "^(\\/)([a-zA-Z0-9._-]*\\w)(\\/)([a-zA-Z0-9._-]*\\w)(\\/{0,1})";

        if (pathInfo.matches(regex) && queryStr == "") {
            return true;
        } else {
            return false;
        }
    }
}
