/*
 * Copyright 2011-2022 Fraunhofer ISE
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

import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BundleHttpContext implements HttpContext {

    private static final Logger logger = LoggerFactory.getLogger(BundleHttpContext.class);

    private final Bundle contextBundle;

    public BundleHttpContext(Bundle contextBundle) {
        this.contextBundle = contextBundle;
    }

    @Override
    public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // TODO: change this for some files..
        return true;
    }

    @Override
    public URL getResource(String name) {
        String pathname = System.getProperty("user.dir") + name;
        if (name.startsWith("/media/")) {
            return findUrl(pathname, "*");
        }
        else if (name.startsWith("/conf/webui/")) {
            return findUrl(pathname, ".conf");
        }
        return contextBundle.getResource(name);

    }

    private URL findUrl(String pathname, String fileEnding) {
        String path = pathname;
        if (!fileEnding.equals("*")) {
            path += fileEnding;
        }
        File file = new File(path);

        if (!file.canRead()) {
            logger.warn("Can not read requested file at {}.", path);
            return null;
        }
        try {
            return file.toURI().toURL();
        } catch (MalformedURLException e) {
            logger.warn("Can not read requested file at {}. {}", path, e);
            return null;
        }
    }

    @Override
    public String getMimeType(String file) {
        if (file.endsWith(".jpg")) {
            return "image/jpeg";
        }
        else if (file.endsWith(".png")) {
            return "image/png";
        }
        else if (file.endsWith(".js")) {
            return "text/javascript";
        }
        else if (file.endsWith(".css")) {
            return "text/css";
        }
        else if (file.endsWith(".html")) {
            return "text/html";
        }
        else if (file.endsWith(".pdf")) {
            return "application/pdf";
        }
        else if (file.startsWith("/conf/webui")) {
            return "application/json";
        }
        else {
            return "text/html";
        }
    }

}
