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
package org.openmuc.framework.server.restws.servlets;

import java.io.BufferedReader;
import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;

public class ServletLib {

    protected final static int PATH_ARRAY_NR = 0;
    protected final static int QUERRY_ARRAY_NR = 1;

    protected static String buildString(BufferedReader br) {
        StringBuilder text = new StringBuilder();
        String line;
        try {
            while ((line = br.readLine()) != null) {
                text.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return text.toString();
    }

    /**
     * Only the first String will be sended over HTTP response.
     * 
     * @param response
     *            HttpServletResponse response
     * @param errorCode
     *            error code
     * @param logger
     *            logger
     * @param msg
     *            message array
     */
    protected static void sendHTTPErrorAndLogWarn(HttpServletResponse response, int errorCode, Logger logger,
            String... msg) {

        try {
            response.sendError(errorCode, msg[0]);
        } catch (IOException e) {
            logger.error("Could not send HTTP Error message.");
            e.printStackTrace();
        }
        StringBuilder warnMessage = new StringBuilder();
        for (String m : msg) {
            warnMessage.append(m);
        }
        logger.warn(warnMessage.toString());
    }

    /**
     * Only the first String will be sended over HTTP response.
     * 
     * @param response
     *            HttpServletResponse response
     * @param errorCode
     *            error code
     * @param logger
     *            logger
     * @param msg
     *            message array
     */
    protected static void sendHTTPErrorAndLogDebug(HttpServletResponse response, int errorCode, Logger logger,
            String... msg) {

        try {
            response.sendError(errorCode, msg[0]);
        } catch (IOException e) {
            logger.error("Could not send HTTP Error message.");
            e.printStackTrace();
        }
        StringBuilder warnMessage = new StringBuilder();
        for (String m : msg) {
            warnMessage.append(m);
        }
        logger.debug(warnMessage.toString());
    }

    /**
     * Logger and HTTP response are the same message.
     * 
     * @param response
     *            HttpServletResponse response
     * @param errorCode
     *            error code
     * @param logger
     *            logger
     * @param msg
     *            message array
     */
    protected static void sendHTTPErrorAndLogErr(HttpServletResponse response, int errorCode, Logger logger,
            String... msg) {

        try {
            StringBuilder sbErrMessage = new StringBuilder();
            for (String m : msg) {
                sbErrMessage.append(m);
            }
            String errMessage = sbErrMessage.toString();
            response.sendError(errorCode, errMessage);
            logger.error(errMessage);
        } catch (IOException e) {
            logger.error("Could not send HTTP Error message.");
            e.printStackTrace();
        }

    }

    protected static String getJsonText(HttpServletRequest request) throws IOException {

        String jsonText = "";
        jsonText = ServletLib.buildString(request.getReader());
        return jsonText;
    }

    protected static String[] getPathInfoArray(String pathInfo) {

        String returnValue[];

        if (pathInfo.length() > 1) {

            pathInfo = pathInfo.replaceFirst("/", "");
            returnValue = pathInfo.split("/");
        }
        else {
            returnValue = new String[] { "/" };
        }
        return returnValue;
    }
}
