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

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openmuc.framework.lib.json.FromJson;
import org.openmuc.framework.lib.json.ToJson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ControlsServlet extends GenericServlet {

    private static final long serialVersionUID = -5635380730045771853L;
    private final static Logger logger = LoggerFactory.getLogger(DriverResourceServlet.class);

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        response.setContentType("application/json");
        String[] pathAndQueryString = checkIfItIsACorrectRest(request, response, logger);

        if (pathAndQueryString != null) {

            String pathInfo = pathAndQueryString[ServletLib.PATH_ARRAY_NR];
            ToJson json = new ToJson();

            if (pathInfo.equals("/")) {

            }
            else {
                String[] pathInfoArray = ServletLib.getPathInfoArray(pathInfo);

                if (pathInfoArray.length == 1) {

                }
                else {
                    ServletLib.sendHTTPErrorAndLogDebug(response, HttpServletResponse.SC_NOT_FOUND, logger,
                            "Requested rest path is not available.", " Path Info = ", request.getPathInfo());
                }

            }
            sendJson(json, response);
        }
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        response.setContentType("application/json");
        String[] pathAndQueryString = checkIfItIsACorrectRest(request, response, logger);

        if (pathAndQueryString != null) {

            String pathInfo = pathAndQueryString[ServletLib.PATH_ARRAY_NR];
            new FromJson(ServletLib.getJsonText(request));

            if (pathInfo.equals("/")) {

            }
            else {
                ServletLib.sendHTTPErrorAndLogDebug(response, HttpServletResponse.SC_NOT_FOUND, logger,
                        "Requested rest path is not available.", " Rest Path = ", request.getPathInfo());
            }
        }
    }

    @Override
    public void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        response.setContentType("application/json");
        String[] pathAndQueryString = checkIfItIsACorrectRest(request, response, logger);

        if (pathAndQueryString != null) {

            String pathInfo = pathAndQueryString[ServletLib.PATH_ARRAY_NR];
            new FromJson(ServletLib.getJsonText(request));

            if (pathInfo.equals("/")) {

            }
            else {
                ServletLib.sendHTTPErrorAndLogDebug(response, HttpServletResponse.SC_NOT_FOUND, logger,
                        "Requested rest path is not available.", " Rest Path = ", request.getPathInfo());
            }
        }
    }

    @Override
    public void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        String[] pathAndQueryString = checkIfItIsACorrectRest(request, response, logger);

        if (pathAndQueryString != null) {

            String pathInfo = pathAndQueryString[ServletLib.PATH_ARRAY_NR];

            new FromJson(ServletLib.getJsonText(request));

            if (pathInfo.equals("/")) {

            }
            else {
                ServletLib.sendHTTPErrorAndLogDebug(response, HttpServletResponse.SC_NOT_FOUND, logger,
                        "Requested rest path is not available.", " Rest Path = ", request.getPathInfo());
            }

        }
        else {
            ServletLib.sendHTTPErrorAndLogDebug(response, HttpServletResponse.SC_NOT_FOUND, logger,
                    "Requested rest path is not available.", " Rest Path = ", request.getPathInfo());
        }
    }

}
