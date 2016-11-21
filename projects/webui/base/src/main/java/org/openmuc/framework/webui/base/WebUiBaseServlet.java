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
package org.openmuc.framework.webui.base;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.openmuc.framework.authentication.AuthenticationService;
import org.openmuc.framework.webui.spi.WebUiPluginService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

@SuppressWarnings("serial")
public final class WebUiBaseServlet extends HttpServlet {

    private final static Logger logger = LoggerFactory.getLogger(WebUiBaseServlet.class);

    private static final int SESSION_TIMEOUT = 300;

    private final WebUiBase webUiBase;

    private final static Gson gson = new Gson();

    public WebUiBaseServlet(WebUiBase webUiBase) {
        this.webUiBase = webUiBase;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        if ("/applications".equals(req.getPathInfo())) {

            if (req.getSession().isNew()) {
                req.getSession().invalidate();
                resp.sendError(401);
                return;
            }

            List<Application> applications = new ArrayList<>();
            for (WebUiPluginService webUiApplication : webUiBase.pluginsByAlias.values()) {
                Application application = new Application();
                application.setAlias(webUiApplication.getAlias());
                application.setName(webUiApplication.getName());
                applications.add(application);
            }
            Type typeOfSrc = new TypeToken<List<Application>>() {
            }.getType();
            logger.debug(gson.toJsonTree(applications, typeOfSrc).toString());
            resp.getWriter().println(gson.toJsonTree(applications, typeOfSrc));
            return;
        }

        InputStream inputStream = getServletContext().getResourceAsStream("page.html");
        OutputStream outputStream = resp.getOutputStream();

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

        if ("/login".equals(req.getPathInfo())) {
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

            // String redirect = req.getParameter("redirect");
            // if (redirect.contains("logout")) {
            // redirect = "/openmuc";
            // }
            // resp.sendRedirect(redirect);
        }
        // else if (req.getPathInfo().equals("/account")) {
        // AuthenticationService auth = webUiBase.getAuthenticationService();
        // String user = (String) req.getSession().getValue("user");
        // String pwd = req.getParameter("pwd");
        // logger.info(user + " is trying to change his account...");
        // if (auth.login(user, pwd)) {
        // if (req.getParameter("change").equals("pwd")) {
        // String newPwd = req.getParameter("newPwd");
        // String rePwd = req.getParameter("rePwd");
        // if (newPwd.equals(rePwd)) {
        // auth.delete(user);
        // auth.register(user, newPwd);
        // logger.info("succeeded! (Password changed)");
        // }
        // else {
        // logger.info("failed! (Password mismatch)");
        // }
        // }
        // else if (req.getParameter("change").equals("user")) {
        // String newUser = req.getParameter("newUser");
        // if (!newUser.equals("") && !auth.contains(newUser) && !newUser.contains(":")) {
        // auth.delete(user);
        // auth.register(newUser, pwd);
        // req.getSession().putValue("user", newUser);
        // logger.info("suceeded! (Username changed to " + newUser + ")\n");
        // }
        // else {
        // logger.info("failed! (Username could not be changed)\n");
        // }
        // }
        // }
        // else {
        // logger.info("failed! (Login failed)\n");
        // }
        // resp.sendRedirect(req.getRequestURI());
        // }
        else {
            doGet(req, resp);
        }

    }
}
