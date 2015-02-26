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

import org.apache.velocity.app.Velocity;
import org.openmuc.framework.authentication.AuthenticationService;
import org.openmuc.framework.webui.spi.ResourceLoader;
import org.openmuc.framework.webui.spi.WebUiPluginService;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class WebUiBase {

    private final static Logger logger = LoggerFactory.getLogger(WebUiBase.class);

    private final Map<String, WebUiPluginService> pluginsByAlias = new ConcurrentHashMap<String, WebUiPluginService>();
    private volatile HttpService httpService;
    private AuthenticationService authService;

    private volatile WebUiBaseServlet servlet;

    protected void activate(ComponentContext context) throws Exception {
        logger.info("Activating WebUI Base");

        Velocity.setProperty("resource.loader", "class");
        Velocity.setProperty("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");

        Velocity.init();

        servlet = new WebUiBaseServlet(new ResourceLoader(context.getBundleContext()), this);

        try {
            httpService.registerResources("/openmuc/css", "/css", null);
            httpService.registerResources("/openmuc/images", "/images", null);
            httpService.registerResources("/openmuc/js", "/js", null);
            httpService.registerServlet("/openmuc", servlet, null, null);
        } catch (Exception e) {
        }

        synchronized (pluginsByAlias) {
            for (WebUiPluginService plugin : pluginsByAlias.values()) {
                registerResources(plugin);
            }
        }

    }

    protected void deactivate(ComponentContext context) {
        logger.info("Deactivating WebUI Base");
        httpService.unregister("/openmuc");
        httpService.unregister("/openmuc/css");
        httpService.unregister("/openmuc/images");
        httpService.unregister("/openmuc/js");
    }

    protected void setHttpService(HttpService httpService) {
        this.httpService = httpService;
    }

    protected void unsetHttpService(HttpService httpService) {
        this.httpService = null;
    }

    protected void setWebUiPluginService(WebUiPluginService uiPlugin) {

        synchronized (pluginsByAlias) {
            if (!pluginsByAlias.containsValue(uiPlugin)) {
                pluginsByAlias.put(uiPlugin.getAlias(), uiPlugin);
                registerResources(uiPlugin);
            }
        }
        logger.info("WebUI plugin registered: " + uiPlugin.getName());
    }

    protected void unsetWebUiPluginService(WebUiPluginService uiPlugin) {
        unregisterResources(uiPlugin);
        pluginsByAlias.remove(uiPlugin.getAlias());
        logger.info("WebUI plugin deregistered: " + uiPlugin.getName());
    }

    protected void setAuthenticationService(AuthenticationService authService) {
        this.authService = authService;
    }

    protected void unsetAuthenticationService(AuthenticationService authService) {
        this.authService = null;
    }

    private void registerResources(WebUiPluginService plugin) {
        if (servlet != null && httpService != null) {
            Set<String> aliases = plugin.getResources().keySet();
            for (String alias : aliases) {
                try {

                    httpService.registerResources("/openmuc/" + plugin.getCategory() + "/" + plugin.getAlias() + "/" + alias,
                                                  plugin.getResources().get(alias), plugin);

                } catch (NamespaceException e) {
                    logger.error("Servlet with alias \"/openmuc/" + plugin.getCategory() + "/" + plugin
                            .getAlias() + "/" + alias + "\" already registered");
                }
            }
        }
    }

    private void unregisterResources(WebUiPluginService plugin) {
        Set<String> aliases = plugin.getResources().keySet();

        for (String alias : aliases) {
            httpService.unregister("/openmuc/" + plugin.getCategory().toString() + "/" + plugin.getAlias() + "/" + alias);
        }
    }

    public WebUiPluginService getPlugin(String alias) {
        return pluginsByAlias.get(alias);
    }

    public Map<String, WebUiPluginService> getPlugins() {
        return pluginsByAlias;
    }

    public AuthenticationService getAuthenticationService() {
        return authService;
    }

}
