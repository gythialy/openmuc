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
package org.openmuc.framework.webui.base;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openmuc.framework.authentication.AuthenticationService;
import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;

public class BundleHttpContext implements HttpContext {

    Bundle contextBundle;
    AuthenticationService authService;

    public BundleHttpContext(Bundle contextBundle, AuthenticationService authService) {
        this.contextBundle = contextBundle;
        this.authService = authService;
    }

    @Override
    public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) throws IOException {
        return true;
    }

    @Override
    public URL getResource(String name) {
        if (name.startsWith("/media/")) {
            File file = new File(System.getProperty("user.dir") + name);
            if (!file.canRead()) {
                return null;
            }
            try {
                return file.toURI().toURL();
            } catch (MalformedURLException e) {
                return null;
            }
        }
        else if (name.startsWith("/conf/webui/")) {
            File file = new File(System.getProperty("user.dir") + name + ".conf");
            if (!file.canRead()) {
                return null;
            }
            try {
                return file.toURI().toURL();
            } catch (MalformedURLException e) {
                return null;
            }
        }
        return contextBundle.getResource(name);

    }

    @Override
    public String getMimeType(String name) {
        return null;
    }

}
