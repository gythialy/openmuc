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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.openmuc.framework.authentication.AuthenticationService;
import org.openmuc.framework.webui.spi.WebUiPluginService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

@SuppressWarnings("serial")
public class WebUiBaseServlet extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(WebUiBaseServlet.class);

    private static final int SESSION_TIMEOUT = 300;

    private final WebUiBase webUiBase;

    public WebUiBaseServlet(WebUiBase webUiBase) {
        this.webUiBase = webUiBase;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String servletPath = req.getServletPath();

        if (servletPath == null) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Path is null.");
        }
        else if ("/applications".equals(servletPath)) {

            if (req.getSession().isNew()) {
                req.getSession().invalidate();
                resp.sendError(401);
                return;
            }

            JsonArray jApplications = new JsonArray();
            for (WebUiPluginService webUiApp : webUiBase.pluginsByAlias.values()) {
                JsonObject app = new JsonObject();
                app.addProperty("alias", webUiApp.getAlias());
                app.addProperty("name", webUiApp.getName());
                jApplications.add(app);
            }

            String applicationsStr = jApplications.toString();

            logger.debug(applicationsStr);

            resp.setContentType("application/json");
            resp.getWriter().println(applicationsStr);
            return;
        }

        InputStream inputStream = getServletContext().getResourceAsStream("page.html");
        OutputStream outputStream = resp.getOutputStream();
        resp.setContentType("text/html");

        copyStream(inputStream, outputStream);

        outputStream.close();
        inputStream.close();
    }

    public static void copyStream(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[1024]; // Adjust if you want
        int bytesRead;
        while ((bytesRead = input.read(buffer)) != -1) {
            output.write(buffer, 0, bytesRead);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if ("/login".equals(req.getServletPath())) {
            String user = req.getParameter("user");
            String pwd = req.getParameter("pwd");

            AuthenticationService auth = webUiBase.getAuthenticationService();
            if (auth.login(user, pwd)) {

                HttpSession session = req.getSession(true); // create a new session
                session.setMaxInactiveInterval(SESSION_TIMEOUT); // and set timeout
                session.setAttribute("user", user);
            }
            else {
                logger.info("login failed!");
                req.getSession().invalidate(); // invalidate the session
                resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            }
        }
        else {
            doGet(req, resp);
        }

    }
}
