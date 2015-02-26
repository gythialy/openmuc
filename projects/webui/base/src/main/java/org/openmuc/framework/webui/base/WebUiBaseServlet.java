/*
 * Copyright 2011-15 Fraunhofer ISE
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

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.tools.generic.EscapeTool;
import org.openmuc.framework.authentication.AuthenticationService;
import org.openmuc.framework.webui.spi.PluginContext;
import org.openmuc.framework.webui.spi.ResourceLoader;
import org.openmuc.framework.webui.spi.View;
import org.openmuc.framework.webui.spi.WebUiPluginService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

@SuppressWarnings("serial")
public final class WebUiBaseServlet extends HttpServlet {

    private final static Logger logger = LoggerFactory.getLogger(WebUiBaseServlet.class);

    private static final String APPLICATIONS = "applications";
    private static final String CONFIGURATION = "configtool";
    private static final String TOOLS = "drivertool";
    private static final int SESSION_TIMEOUT = 300; // time in seconds, till
    // session expires

    private final ResourceLoader loader;
    private final WebUiBase pluginManager;

    public WebUiBaseServlet(ResourceLoader loader, WebUiBase pluginManager) {
        this.loader = loader;
        this.pluginManager = pluginManager;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            String pathInfo = req.getPathInfo();
            if (pathInfo == null) {
                pathInfo = "";
            }

            List<MenuItem> navigation = getNavigationMenu(pathInfo);

            List<String> styleSheets = new ArrayList<String>();
            styleSheets.add("/openmuc/css/style.css");

            List<String> javaScripts = new ArrayList<String>();
            javaScripts.add("/openmuc/js/jquery-1.7.1.min.js");

            VelocityContext vc = new VelocityContext();

            SystemInfo sysInfo = new SystemInfo();

			/* populate the root context for all templates */
            vc.put("date", new Date());
            vc.put("navigationItems", navigation);
            vc.put("stylesheets", styleSheets);
            vc.put("javascripts", javaScripts);
            vc.put("user", req.getSession().getValue("user"));
            vc.put("esc", new EscapeTool());

            if (req.getSession().isNew() || pathInfo.equals("/logout")) {
                req.getSession().invalidate(); // Make sure that session stays
                // invalid
                vc.put("application", "Welcome");
                vc.put("page", "Login");
                if (req.getQueryString() == null) {
                    vc.put("path", req.getRequestURI());
                } else {
                    vc.put("path", req.getRequestURI() + '?' + req.getQueryString());
                }
                vc.put("user", "Logged out");
                vc.put("content_template", loader.getResourceAsString("login.html"));
            } else if (pathInfo.equals("/account")) {
                vc.put("application", "Control Center");
                vc.put("page", "Account");
                vc.put("content_template", loader.getResourceAsString("account.html"));
            } else if (pathInfo.startsWith("/system")) {
                vc.put("application", "Control Center");
                vc.put("page", "System Information");
                vc.put("system", sysInfo);
                vc.put("content_template", loader.getResourceAsString("sysinfo.html"));
            } else if (pathInfo.equals("/configuration") || pathInfo.equals("/tools") || pathInfo.equals("/applications")) {

                vc.put("application", "Control Center");
                vc.put("page", "Overview");
                vc.put("content_template", loader.getResourceAsString("navinfo.html"));
            } else {
                PluginContextImpl pluginContext = new PluginContextImpl(pathInfo, req, resp);

                WebUiPluginService plugin = pluginManager.getPlugin(pluginContext.getApplicationAlias());

                if (plugin != null) {
                    vc.put("applicationPath", pluginContext.getApplicationPath());
                    vc.put("application", plugin.getName());
                    View view = plugin.getContentView(req, pluginContext);

                    if (view.getRedirectLocation() != null) {
                        resp.sendRedirect(view.getRedirectLocation());
                        return;
                    }

                    if (view.getViewType() != null) {
                        if (view.getViewType() == View.viewtype.AJAX) {
                            HashMap<String, Object> appContext = view.getContext();
                            for (String key : appContext.keySet()) {
                                vc.put(key, appContext.get(key));
                            }

                            vc.put("ajax_content", view.getTemplate());

                            StringWriter writer = new StringWriter();
                            resp.setContentType("text/plain");
                            Velocity.mergeTemplate("ajax.html", RuntimeConstants.ENCODING_DEFAULT, vc, writer);
                            /*
                             * Important for SSL, without '\n' and flush() the content will be wrong
							 */
                            PrintWriter out = resp.getWriter();
                            out.append('\n');
                            out.print(writer);
                            out.close();

                            return;
                        }

                    }

                    vc.put("page", view.getPage());
                    vc.put("content_template", view.getTemplate());

					/*
					 * Add application stylesheets and javascript resources to context
					 */
					/* differing between absolute and relative paths */
                    if (view.getStyleSheets() != null) {
                        for (String appStyleSheet : view.getStyleSheets()) {
                            if (appStyleSheet.startsWith("/")) {
                                styleSheets.add(appStyleSheet);
                            } else {
                                styleSheets.add(pluginContext.getApplicationPath() + "/" + appStyleSheet);
                            }
                        }
                    }
                    if (view.getJavaScripts() != null) {
						/*
						 * Adding Javascripts to page.html starting with "/" -> absolute Path without "/" -> relative
						 * Plugin Path
						 */
                        for (String appJavaScript : view.getJavaScripts()) {
                            if (appJavaScript.startsWith("/")) {
                                javaScripts.add(appJavaScript);
                            } else {
                                javaScripts.add(pluginContext.getApplicationPath() + "/" + appJavaScript);
                            }
                        }
                    }

                    HashMap<String, Object> appContext = view.getContext();

                    for (String key : appContext.keySet()) {
                        vc.put(key, appContext.get(key));
                    }
                } else {
                    vc.put("application", "Control Center");
                    vc.put("page", "Welcome");
                    vc.put("content_template", loader.getResourceAsString("content.html"));

                }
            }
            StringWriter writer = new StringWriter();

            resp.setContentType("text/html");
            Velocity.mergeTemplate("page.html", RuntimeConstants.ENCODING_DEFAULT, vc, writer);
			/*
			 * Important for SSL, without '\n' and flush() the content will be wrong
			 */
            PrintWriter out = resp.getWriter();
            out.append('\n');
            out.print(writer);
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (req.getPathInfo().equals("/login")) {
            String user = req.getParameter("user");
            String pwd = req.getParameter("pwd");

            AuthenticationService auth = pluginManager.getAuthenticationService();
            if (auth.login(user, pwd)) {

                HttpSession session = req.getSession(true); // create a new session
                session.setMaxInactiveInterval(SESSION_TIMEOUT); // and set timeout
                session.putValue("user", user);
            } else {
                logger.info("login failed!");
                req.getSession().invalidate(); // invalidate the session
            }

            String redirect = req.getParameter("redirect");
            if (redirect.contains("logout")) {
                redirect = "/openmuc";
            }
            resp.sendRedirect(redirect);
        } else if (req.getPathInfo().equals("/account")) {
            AuthenticationService auth = pluginManager.getAuthenticationService();
            String user = (String) req.getSession().getValue("user");
            String pwd = req.getParameter("pwd");
            logger.info(user + " is trying to change his account...");
            if (auth.login(user, pwd)) {
                if (req.getParameter("change").equals("pwd")) {
                    String newPwd = req.getParameter("newPwd");
                    String rePwd = req.getParameter("rePwd");
                    if (newPwd.equals(rePwd)) {
                        auth.delete(user);
                        auth.register(user, newPwd);
                        logger.info("succeeded! (Password changed)");
                    } else {
                        logger.info("failed! (Password mismatch)");
                    }
                } else if (req.getParameter("change").equals("user")) {
                    String newUser = req.getParameter("newUser");
                    if (!newUser.equals("") && !auth.contains(newUser) && !newUser.contains(":")) {
                        auth.delete(user);
                        auth.register(newUser, pwd);
                        req.getSession().putValue("user", newUser);
                        logger.info("suceeded! (Username changed to " + newUser + ")\n");
                    } else {
                        logger.info("failed! (Username could not be changed)\n");
                    }
                }
            } else {
                logger.info("failed! (Login failed)\n");
            }
            resp.sendRedirect(req.getRequestURI());
        } else {
            doGet(req, resp);
        }

    }

    private List<MenuItem> getNavigationMenu(String pathInfo) {
        ArrayList<MenuItem> menu = new ArrayList<MenuItem>();

        MenuItem configuration = new MenuItem("Configuration", CONFIGURATION);
        MenuItem applications = new MenuItem("Applications", APPLICATIONS);
        MenuItem tools = new MenuItem("Tools", TOOLS);
        MenuItem system = new MenuItem("System", "system");

        if (pathInfo.equals("/system")) {
            system.setActive();
        }

        menu.add(system);
        menu.add(configuration);
        ;
        menu.add(applications);
        menu.add(tools);

        Map<String, WebUiPluginService> plugins = pluginManager.getPlugins();

        Set<String> aliases = plugins.keySet();

        PluginContext pluginContext = new PluginContextImpl(pathInfo, null, null);

        for (String alias : aliases) {
            WebUiPluginService plugin = plugins.get(alias);
            if (plugin != null) {
                MenuItem menuItem = new MenuItem(plugin.getName(), plugin.getCategory().toString() + "/" + plugin.getAlias(),
                                                 plugin.getDescription());

                if (plugin.getAlias().equals(pluginContext.getApplicationAlias())) {
                    menuItem.setActive();
                }

                switch (plugin.getCategory()) {
                    case APPLICATION:
                        applications.addSubItem(menuItem);
                        break;
                    case CONFIGTOOL:
                        configuration.addSubItem(menuItem);
                        break;
                    case DRIVERTOOL:
                        tools.addSubItem(menuItem);
                        break;
                    default:
                        menu.add(new MenuItem(plugin.getName(), plugin.getAlias()));
                }
            }
        }

        if (pathInfo != null) {
            if (pathInfo.startsWith("/" + CONFIGURATION)) {
                configuration.setActive();
            } else if (pathInfo.startsWith("/" + APPLICATIONS)) {
                applications.setActive();
            } else if (pathInfo.startsWith("/" + TOOLS)) {
                tools.setActive();
            }
        }

        return menu;
    }

}
