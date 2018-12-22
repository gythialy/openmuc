/*
 * Copyright 2011-18 Fraunhofer ISE
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
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openmuc.framework.authentication.AuthenticationService;
import org.openmuc.framework.config.ConfigChangeListener;
import org.openmuc.framework.config.ConfigService;
import org.openmuc.framework.config.RootConfig;
import org.openmuc.framework.dataaccess.DataAccessService;
import org.openmuc.framework.lib.json.ToJson;
import org.openmuc.framework.server.restws.RestServer;
import org.slf4j.Logger;

public abstract class GenericServlet extends HttpServlet implements ConfigChangeListener {

    private static final long serialVersionUID = 4041357804530863512L;
    private static final Charset CHARSET = StandardCharsets.UTF_8;

    private static DataAccessService dataAccess;
    private static ConfigService configService;
    private static AuthenticationService authenticationService;
    private static RootConfig rootConfig;

    @Override
    public void init() throws ServletException {
        handleDataAccessService(RestServer.getDataAccessService());
        handleConfigService(RestServer.getConfigService());
        handleRootConfig(configService.getConfig(this));
        handleAuthenticationService(RestServer.getAuthenticationService());
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    }

    @Override
    public void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    }

    @Override
    public void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
    }

    @Override
    public void destroy() {
    }

    @Override
    public void configurationChanged() {
        rootConfig = configService.getConfig();
    }

    void sendJson(ToJson json, HttpServletResponse response) throws ServletException, IOException {
        OutputStream outStream = response.getOutputStream();
        if (json != null) {
            String jsonString = json.toString();
            outStream.write(jsonString.getBytes(CHARSET));
        }
        outStream.flush();
        outStream.close();
    }

    String[] checkIfItIsACorrectRest(HttpServletRequest request, HttpServletResponse response, Logger logger) {
        String pathAndQueryString[] = new String[2];

        String pathInfo = request.getPathInfo();
        String queryStr = request.getQueryString();

        if (pathInfo == null) {
            pathInfo = "/";
        }
        if (queryStr == null) {
            queryStr = "";
        }

        /* Accept only "application/json" and null. Null is a browser request. */
        if (request.getContentType() != null && !request.getContentType().startsWith("application/json")) {
            ServletLib.sendHTTPErrorAndLogDebug(response, HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE, logger,
                    "Requested rest was not a json media type. Requested media type is: " + request.getContentType());
            pathAndQueryString = null;
        }
        else {
            pathAndQueryString[0] = pathInfo;
            pathAndQueryString[1] = queryStr;
        }
        return pathAndQueryString;
    }

    synchronized DataAccessService handleDataAccessService(DataAccessService dataAccessService) {
        if (dataAccessService != null) {
            dataAccess = dataAccessService;
        }
        return dataAccess;
    }

    synchronized ConfigService handleConfigService(ConfigService configServ) {
        if (configServ != null) {
            configService = configServ;
        }
        return configService;
    }

    synchronized AuthenticationService handleAuthenticationService(AuthenticationService authServ) {
        if (authServ != null) {
            authenticationService = authServ;
        }
        return authenticationService;
    }

    synchronized RootConfig handleRootConfig(RootConfig rootConf) {
        if (rootConf != null) {
            rootConfig = rootConf;
        }
        return rootConfig;
    }

}
