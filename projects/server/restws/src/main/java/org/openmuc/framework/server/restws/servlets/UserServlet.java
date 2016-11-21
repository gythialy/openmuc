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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openmuc.framework.authentication.AuthenticationService;
import org.openmuc.framework.lib.json.Const;
import org.openmuc.framework.lib.json.FromJson;
import org.openmuc.framework.lib.json.ToJson;
import org.openmuc.framework.lib.json.restObjects.RestUserConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserServlet extends GenericServlet {

    private static final long serialVersionUID = -5635380730045771853L;
    private final static Logger logger = LoggerFactory.getLogger(DriverResourceServlet.class);

    private AuthenticationService authenticationService;

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        response.setContentType("application/json");
        String[] pathAndQueryString = checkIfItIsACorrectRest(request, response, logger);

        if (pathAndQueryString != null) {

            setServices();
            String pathInfo = pathAndQueryString[ServletLib.PATH_ARRAY_NR];
            ToJson json = new ToJson();

            if (pathInfo.equals("/")) {
                Set<String> userSet = authenticationService.getAllUsers();
                List<String> userList = new ArrayList<>();
                userList.addAll(userSet);
                json.addStringList(Const.USERS, userList);
            }
            else {
                String[] pathInfoArray = ServletLib.getPathInfoArray(pathInfo);

                if (pathInfoArray.length == 1) {

                    String userID = pathInfoArray[0].replace("/", "");

                    if (userID.equalsIgnoreCase(Const.GROUPS)) {

                        List<String> groupList = new ArrayList<>();
                        groupList.add(""); // TODO: add real groups, if groups exists in OpenMUC
                        json.addStringList(Const.GROUPS, groupList);
                    }
                    else if (authenticationService.contains(userID)) {
                        RestUserConfig restUserConfig = new RestUserConfig();
                        restUserConfig.setId(userID);
                        restUserConfig.setPassword("*****");
                        restUserConfig.setGroups(new String[] { "" });
                        restUserConfig.setDescription("");
                        json.addRestUserConfig(restUserConfig);
                    }

                    else {
                        ServletLib.sendHTTPErrorAndLogDebug(response, HttpServletResponse.SC_NOT_FOUND, logger,
                                "User does not exist.", " User = ", userID);
                    }

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

            setServices();
            String pathInfo = pathAndQueryString[ServletLib.PATH_ARRAY_NR];
            FromJson json = new FromJson(ServletLib.getJsonText(request));

            if (pathInfo.equals("/")) {
                RestUserConfig userConfig = json.getRestUserConfig();

                if (authenticationService.contains(userConfig.getId())) {
                    ServletLib.sendHTTPErrorAndLogDebug(response, HttpServletResponse.SC_NOT_FOUND, logger,
                            "User already exists.", " User = ", userConfig.getId());
                }
                else if (userConfig.getPassword() == null || userConfig.getPassword().equals("")) {
                    ServletLib.sendHTTPErrorAndLogDebug(response, HttpServletResponse.SC_PRECONDITION_FAILED, logger,
                            "Password is mandatory.");
                }
                else {
                    authenticationService.register(userConfig.getId(), userConfig.getPassword());
                }

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

            setServices();
            String pathInfo = pathAndQueryString[ServletLib.PATH_ARRAY_NR];
            FromJson json = new FromJson(ServletLib.getJsonText(request));

            if (pathInfo.equals("/")) {
                RestUserConfig userConfig = json.getRestUserConfig();

                if (userConfig.getPassword() == null || userConfig.getPassword().equals("")) {
                    ServletLib.sendHTTPErrorAndLogDebug(response, HttpServletResponse.SC_PRECONDITION_FAILED, logger,
                            "Password is mandatory.");
                }
                else if (userConfig.getOldPassword() == null || userConfig.getOldPassword().equals("")) {
                    ServletLib.sendHTTPErrorAndLogDebug(response, HttpServletResponse.SC_PRECONDITION_FAILED, logger,
                            "Old password is mandatory.");
                }
                else if (authenticationService.contains(userConfig.getId())) {
                    String id = userConfig.getId();
                    if (authenticationService.login(id, userConfig.getOldPassword())) {
                        authenticationService.register(id, userConfig.getPassword());
                    }
                    else {
                        ServletLib.sendHTTPErrorAndLogDebug(response, HttpServletResponse.SC_UNAUTHORIZED, logger,
                                "Old password is wrong.");
                    }
                }
                else {
                    ServletLib.sendHTTPErrorAndLogDebug(response, HttpServletResponse.SC_NOT_FOUND, logger,
                            "User does not exist.", " User = ", userConfig.getId());
                }
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

            setServices();
            String pathInfo = pathAndQueryString[ServletLib.PATH_ARRAY_NR];

            FromJson json = new FromJson(ServletLib.getJsonText(request));

            if (pathInfo.equals("/")) {

                RestUserConfig userConfig = json.getRestUserConfig();
                String userID = userConfig.getId();

                if (authenticationService.contains(userID)) {
                    authenticationService.delete(userID);
                }
                else {
                    ServletLib.sendHTTPErrorAndLogDebug(response, HttpServletResponse.SC_NOT_FOUND, logger,
                            "Requested user does not exist.", " User = ", userID);
                }
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

    private void setServices() {

        this.authenticationService = handleAuthenticationService(null);
    }

}
