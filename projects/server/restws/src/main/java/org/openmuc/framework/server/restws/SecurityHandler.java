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
package org.openmuc.framework.server.restws;

import java.io.IOException;
import java.net.URL;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.openmuc.framework.authentication.AuthenticationService;
import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;

public class SecurityHandler implements HttpContext {

    Bundle contextBundle;
    AuthenticationService authService;

    public SecurityHandler(Bundle contextBundle, AuthenticationService authService) {
        this.contextBundle = contextBundle;
        this.authService = authService;
    }

    @Override
    public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!authenticated(request)) {
            response.setHeader("WWW-Authenticate", "BASIC realm=\"private area\"");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        return true;
    }

    private boolean authenticated(HttpServletRequest request) {
        if (request.getMethod().equals("OPTIONS")) {
            return true;
        }
        String authzHeader = request.getHeader("Authorization");
        if (authzHeader == null) {
            return false;
        }
        String usernameAndPassword;
        try {
            usernameAndPassword = new String(Base64.decodeBase64(authzHeader.substring(6)));
        } catch (ArrayIndexOutOfBoundsException e) {
            return false;
        }

        int userNameIndex = usernameAndPassword.indexOf(':');
        String username = usernameAndPassword.substring(0, userNameIndex);
        String password = usernameAndPassword.substring(userNameIndex + 1);
        return authService.login(username, password);
    }

    @Override
    public URL getResource(String name) {
        return contextBundle.getResource(name);
    }

    @Override
    public String getMimeType(String name) {
        return null;
    }

}
