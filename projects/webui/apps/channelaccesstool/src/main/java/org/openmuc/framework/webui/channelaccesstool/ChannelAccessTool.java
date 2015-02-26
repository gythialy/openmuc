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

package org.openmuc.framework.webui.channelaccesstool;

import org.openmuc.framework.config.ConfigService;
import org.openmuc.framework.config.DeviceConfig;
import org.openmuc.framework.config.RootConfig;
import org.openmuc.framework.data.DoubleValue;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.dataaccess.DataAccessService;
import org.openmuc.framework.webui.spi.*;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

public final class ChannelAccessTool implements WebUiPluginService {

    private final static Logger logger = LoggerFactory.getLogger(ChannelAccessTool.class);

    private BundleContext context;
    private ResourceLoader loader;
    private ConfigService configService;
    private DataAccessService dataAccessService;

    protected void activate(ComponentContext context) {
        this.context = context.getBundleContext();
        loader = new ResourceLoader(context.getBundleContext());
    }

    protected void setDataAccessService(DataAccessService dataAccessService) {
        this.dataAccessService = dataAccessService;
    }

    protected void unsetDataAccessService(DataAccessService dataAccessService) {
        this.dataAccessService = null;
    }

    protected void setConfigService(ConfigService configService) {
        this.configService = configService;
    }

    protected void unsetConfigService(ConfigService configService) {
        this.configService = null;
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
        return "channelaccesstool";
    }

    @Override
    public String getName() {
        return "Channel Access Tool";
    }

    @Override
    public String getDescription() {
        return "Shows the current values of the sensors, and allows you to set output values.";
    }

    @Override
    public View getContentView(HttpServletRequest request, PluginContext context) {
        String template = "";

        if (context.getLocalPath().equals("/getValue")) {

            String id = request.getParameter("id");
            Record record = dataAccessService.getChannel(id).getLatestRecord();
            String serializedJSONContent = recordToJson(record);
            AjaxView view = new AjaxView(serializedJSONContent);
            return view;
        }
        if (context.getLocalPath().equals("/setValue")) {
            String id = request.getParameter("id");
            Double value;
            try {
                value = new Double(request.getParameter("value"));
            } catch (NumberFormatException e) {
                value = 0.0;
            }
            dataAccessService.getChannel(id).write(new DoubleValue(value));
            return new RedirectView(context.getApplicationPath());
        }
        if (context.getLocalPath().equals("/access")) {
            try {
                String[] sDevices = request.getParameterValues("devices");
                List<DeviceConfig> devices = new ArrayList<DeviceConfig>();
                RootConfig config = configService.getConfig();
                for (String deviceId : sDevices) {
                    devices.add(config.getDevice(deviceId));
                }
                template = loader.getResourceAsString("access.html");
                MeasureView view = new MeasureView(template);
                view.addToContext("devices", devices);
                return view;
            } catch (Exception e) {
                return new RedirectView(context.getApplicationPath());
            }
        } else {
            template = loader.getResourceAsString("index.html");
            MeasureView view = new MeasureView(template);
            view.addToContext("drivers", configService.getConfig().getDrivers());
            return view;
        }
    }

    @Override
    public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) throws IOException {
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
        return PluginCategory.APPLICATION;
    }

    private String recordToJson(Record record) {
        return "{\"timestamp\":" + record.getTimestamp() + ",\"value\":" + record.getValue() + ",\"flag\":\"" + record.getFlag() + "\"}";
    }

}
