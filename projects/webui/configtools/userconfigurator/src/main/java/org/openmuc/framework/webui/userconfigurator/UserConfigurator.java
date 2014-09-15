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
package org.openmuc.framework.webui.userconfigurator;

import org.openmuc.framework.authentication.AuthenticationService;
import org.openmuc.framework.webui.spi.*;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URL;
import java.util.Hashtable;
import java.util.Set;

public class UserConfigurator implements WebUiPluginService {

    private static final Logger logger = LoggerFactory.getLogger(UserConfigurator.class);

    private BundleContext context;
    private ResourceLoader loader;
    private AuthenticationService authService;

    protected void activate(ComponentContext context) {
        this.context = context.getBundleContext();
        loader = new ResourceLoader(context.getBundleContext());
    }

    protected void setAuthService(AuthenticationService service) {
        authService = service;
    }

    protected void unsetAuthService(AuthenticationService service) {
        authService = null;
    }

    @Override
    public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        return true;
    }

    @Override
    public URL getResource(String name) {
        return context.getBundle().getResource(name);
    }

    @Override
    public String getMimeType(String name) {
        return null;
    }

    @Override
    public String getName() {
        return "User Configurator";
    }

    @Override
    public String getAlias() {
        return "userconfigurator";
    }

    @Override
    public String getDescription() {
        return "Configuration utility for adding and deleting users";
    }

    @Override
    public PluginCategory getCategory() {
        return PluginCategory.CONFIGTOOL;
    }

    @Override
    public View getContentView(HttpServletRequest request, PluginContext context) {
        View view = null;

        if (context.getLocalPath().equals("/add")) {
            view = new AddUserView(loader);
        } else if (context.getLocalPath().equals("/confirmadd")) {
            String username = request.getParameter("name");
            String pw = request.getParameter("pw");
            String pwconfirm = request.getParameter("pwconfirm");

            String message = "";

            if (username == null || pw == null | pwconfirm == null) {
                message = "Please fill out all input fields";
            } else if (authService.contains(username)) {
                message = "A user with the name '" + username + "' already exists";
            } else if (!pw.equals(pwconfirm)) {
                message = "Password and confirm mismatch";
            } else {
                authService.register(username, pw);
                message = "User created";
            }

            Set<String> users = authService.getAllUsers();
            view = new UserListView(users, loader, message);
        } else if (context.getLocalPath().equals("/remove")) {
            String username = request.getParameter("id");

            String message = "";

            if (username == null) {
                message = "No user name. Delete aborted";
            } else if (!authService.contains(username)) {
                message = "A user with name '" + username + "' does not exist";
            } else if (request.getSession().getValue("user").equals(username)) {
                message = "You cannot delete yourself";
            } else {
                authService.delete(username);
                message = "User '" + username + "' removed from system";
            }

            Set<String> users = authService.getAllUsers();
            view = new UserListView(users, loader, message);
        } else {
            // Per default, show user list
            Set<String> users = authService.getAllUsers();
            view = new UserListView(users, loader, "");
        }

        return view;

    }

    @Override
    public Hashtable<String, String> getResources() {
        Hashtable<String, String> resources = new Hashtable<String, String>();

        resources.put("css", "css");

        return resources;
    }

}
