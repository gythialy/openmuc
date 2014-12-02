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
package org.openmuc.framework.webui.channelconfigurator;

import org.openmuc.framework.config.ConfigService;
import org.openmuc.framework.webui.channelconfigurator.conf.*;
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
import java.util.Map;

public final class ChannelConfiguratorTool implements WebUiPluginService {

    private final static Logger logger = LoggerFactory.getLogger(ChannelConfiguratorTool.class);

    private final Config config = new Config();
    private BundleContext context;

    private DriversConfigurator driversConfigurator;
    private DevicesConfigurator devicesConfigurator;
    private ChannelsConfigurator channelsConfigurator;
    private OptionsConfigurator optionsConfigurator;

    protected void activate(ComponentContext context) {
        this.context = context.getBundleContext();
        config.initContext(context);
    }

    protected void setConfigService(ConfigService configService) {
        config.initConfigService(configService);
    }

    protected void unsetConfigService(ConfigService configService) {
        // this.configService = null;
    }

    @Override
    public Hashtable<String, String> getResources() {
        Hashtable<String, String> resources = new Hashtable<String, String>();

        resources.put("css", "css");
        resources.put("js", "js");
        resources.put("images", "images");

        return resources;
    }

    @Override
    public String getAlias() {
        return "channelconfigurator";
    }

    @Override
    public String getName() {
        return "Channel Configurator";
    }

    @Override
    public String getDescription() {
        return "Configuration utility for the devices and channels.";
    }

    @Override
    public View getContentView(HttpServletRequest request, PluginContext context) {

        View view = null;
        Content content = null;
        try {
            Configurator configurator = null;
            if (context.getLocalPath().startsWith(MenuItem.DEVICES.getPath())) {
                if (devicesConfigurator == null) {
                    devicesConfigurator = new DevicesConfigurator(config);
                }
                configurator = devicesConfigurator;
            } else if (context.getLocalPath().startsWith(MenuItem.CHANNELS.getPath())) {
                if (channelsConfigurator == null) {
                    channelsConfigurator = new ChannelsConfigurator(config);
                }
                configurator = channelsConfigurator;
            } else if (context.getLocalPath().startsWith(MenuItem.OPTIONS.getPath())) {
                if (optionsConfigurator == null) {
                    optionsConfigurator = new OptionsConfigurator(config);
                }
                configurator = optionsConfigurator;
            } else {
                if (driversConfigurator == null) {
                    driversConfigurator = new DriversConfigurator(config);
                }
                configurator = driversConfigurator;
            }

            content = configurator.getContent(context.getLocalPath(), request);

        }
        catch (Exception e) {
            logger.warn("Exception getting content", e);
            content = Content.createErrorMessage(e, null);
        }

        if (content.isRedirect()) {
            view = new RedirectView(context.getApplicationPath() + content.getRedirect());
        } else if (content.isAjax()) {
            view = new AjaxView(content.getHtml());
        } else {
            try {
                view = new WebconfigView(content.getTitle(),
                                         config.getLoader().getResourceAsString("index.html"));

                ((WebconfigView) view).addToContext("content", content);

                for (Map.Entry<String, Object> entry : content.getContext().entrySet()) {
                    ((WebconfigView) view).addToContext(entry.getKey(), entry.getValue());
                }
            }
            catch (ProcessRequestException e) {
                logger.error("not initialized", e);
            }
        }

        return view;
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
    public PluginCategory getCategory() {
        return PluginCategory.CONFIGTOOL;
    }

}
