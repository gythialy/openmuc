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

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.openmuc.framework.authentication.AuthenticationService;
import org.openmuc.framework.webui.spi.WebUiPluginService;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public final class WebUiBase {

    private static final Logger logger = LoggerFactory.getLogger(WebUiBase.class);

    final Map<String, WebUiPluginService> pluginsByAlias = new ConcurrentHashMap<>();
    final Map<String, WebUiPluginService> pluginsByAliasDeactivated = new ConcurrentHashMap<>();

    @Reference
    private HttpService httpService;

    @Reference
    private AuthenticationService authService;

    private volatile WebUiBaseServlet servlet;

    @Activate
    protected void activate(BundleContext context) {
        logger.info("Activating WebUI Base");

        servlet = new WebUiBaseServlet(this);
        servlet.setAuthentification(authService);

        BundleHttpContext bundleHttpContext = new BundleHttpContext(context.getBundle());

        try {
            httpService.registerResources("/app", "/app", bundleHttpContext);
            httpService.registerResources("/assets", "/assets", bundleHttpContext);
            httpService.registerResources("/openmuc/css", "/css", bundleHttpContext);
            httpService.registerResources("/openmuc/images", "/images", bundleHttpContext);
            httpService.registerResources("/openmuc/html", "/html", bundleHttpContext);
            httpService.registerResources("/openmuc/js", "/js", bundleHttpContext);
            httpService.registerResources("/media", "/media", bundleHttpContext);
            httpService.registerResources("/conf/webui", "/conf/webui", bundleHttpContext);
            httpService.registerServlet("/", servlet, null, bundleHttpContext);
        } catch (Exception e) {
        }

        synchronized (pluginsByAlias) {
            for (WebUiPluginService plugin : pluginsByAlias.values()) {
                registerResources(plugin);
            }
        }
    }

    @Deactivate
    protected void deactivate() {
        logger.info("Deactivating WebUI Base");

        httpService.unregister("/app");
        httpService.unregister("/assets");
        httpService.unregister("/openmuc/css");
        httpService.unregister("/openmuc/images");
        httpService.unregister("/openmuc/html");
        httpService.unregister("/openmuc/js");
        httpService.unregister("/media");
        httpService.unregister("/conf/webui");
        httpService.unregister("/");
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void setWebUiPluginService(WebUiPluginService uiPlugin) {

        synchronized (pluginsByAlias) {
            if (!pluginsByAlias.containsValue(uiPlugin)) {
                pluginsByAlias.put(uiPlugin.getAlias(), uiPlugin);
                registerResources(uiPlugin);
            }
        }
    }

    protected void unsetWebUiPluginService(WebUiPluginService uiPlugin) {
        unregisterResources(uiPlugin);
        pluginsByAlias.remove(uiPlugin.getAlias());
        logger.info("WebUI plugin deregistered: {}.", uiPlugin.getName());
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setAuthentificationService(AuthenticationService authService) {
        this.authService = authService;
    }

    protected void unsetAuthentificationService(AuthenticationService authService) {

    }

    protected void unsetWebUiPluginServiceByAlias(String alias) {
        WebUiPluginService toRemovePlugin = pluginsByAlias.remove(alias);
        pluginsByAliasDeactivated.put(alias, toRemovePlugin);
    }

    protected void restoreWebUiPlugin(String alias) {
        WebUiPluginService toAddPlugin = pluginsByAliasDeactivated.remove(alias);
        pluginsByAlias.put(alias, toAddPlugin);
    }

    private void registerResources(WebUiPluginService plugin) {
        if (servlet == null || httpService == null) {
            logger.warn("Can't register WebUI plugin {}.", plugin.getName());
            return;
        }

        BundleHttpContext bundleHttpContext = new BundleHttpContext(plugin.getContextBundle());

        Set<String> aliases = plugin.getResources().keySet();
        for (String alias : aliases) {
            try {
                httpService.registerResources("/" + plugin.getAlias() + "/" + alias, plugin.getResources().get(alias),
                        bundleHttpContext);
            } catch (NamespaceException e) {
                logger.error("Servlet with alias \"/{}/{}\" already registered.", plugin.getAlias(), alias);
            }
        }
        logger.info("WebUI plugin registered: " + plugin.getName());
    }

    private void unregisterResources(WebUiPluginService plugin) {
        Set<String> aliases = plugin.getResources().keySet();

        for (String alias : aliases) {
            httpService.unregister("/" + plugin.getAlias() + "/" + alias);
        }
    }
}
