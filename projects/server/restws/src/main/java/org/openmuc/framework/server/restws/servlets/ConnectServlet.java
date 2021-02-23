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
package org.openmuc.framework.server.restws.servlets;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConnectServlet extends GenericServlet {

    private static final String NOT_FOUND = "Not found.";
    private static final long serialVersionUID = -2248093375930139043L;
    private static final Logger logger = LoggerFactory.getLogger(DriverResourceServlet.class);

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType(APPLICATION_JSON);
        String[] pathAndQueryString = checkIfItIsACorrectRest(request, response, logger);

        if (pathAndQueryString == null) {
            return;
        }

        String pathInfo = pathAndQueryString[ServletLib.PATH_ARRAY_NR];

        if (pathInfo.equals("/")) {
            // do nothing only send 200 SC_OK
            sendJson(null, response);
        }
        else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, NOT_FOUND);
        }
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.sendError(HttpServletResponse.SC_NOT_FOUND, NOT_FOUND);
    }

    @Override
    public void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.sendError(HttpServletResponse.SC_NOT_FOUND, NOT_FOUND);
    }

    @Override
    public void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.sendError(HttpServletResponse.SC_NOT_FOUND, NOT_FOUND);
    }

}
