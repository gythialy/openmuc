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

package org.openmuc.framework.webui.bundleconfigurator;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.openmuc.framework.webui.spi.*;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.metatype.MetaTypeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

/*
 * For a president store osgi.clean must be set to false
 *
 * 		osgi.clean=false
 *
 * and you can not use org.ops4j.pax.configmanager
 *
 */

public final class BundleConfigurator implements WebUiPluginService {

    private final static Logger logger = LoggerFactory.getLogger(BundleConfigurator.class);

    private BundleContext context;
    private ResourceLoader loader;
    private String message = null;
    private MetaTypeService metaTypeService;
    private ConfigurationAdmin configAdmin;

    protected void activate(ComponentContext context) {
        this.context = context.getBundleContext();
        loader = new ResourceLoader(context.getBundleContext());
        message = null;
    }

    public void stop(BundleContext bc) throws Exception {

    }

    @Override
    public Hashtable<String, String> getResources() {
        Hashtable<String, String> resources = new Hashtable<String, String>();

        resources.put("css", "css");

        return resources;
    }

    @Override
    public String getAlias() {
        return "bundleconfigurator";
    }

    @Override
    public String getName() {
        return "Bundle Configurator";
    }

    @Override
    public String getDescription() {
        return "Configuration utility for the OSGI bundles.";
    }

    @Override
    public View getContentView(HttpServletRequest request, PluginContext pluginContext) {
        if (pluginContext.getLocalPath().equals("/edit")) {
            Bundle bundle = getBundleById(request.getParameter("id"));

            BundleConfiguration bundleConfiguration = null;
            try {
                bundleConfiguration = new BundleConfiguration(bundle, configAdmin, metaTypeService);
            } catch (Exception e) {
                message = "Error: " + e.getMessage();
                return new RedirectView(pluginContext.getApplicationPath());
            }

            return new ConfigurationView(bundle, bundleConfiguration, metaTypeService, loader);

        } else if (pluginContext.getLocalPath().equals("/changebundle")) {
            String id = request.getParameter("id");
            Bundle bundle = getBundleById(id);
            try {
                BundleConfiguration bundleConfiguration = new BundleConfiguration(bundle, configAdmin, metaTypeService);
                Dictionary<String, String> bundleProperties = new Hashtable<String, String>();

                String[] keys = request.getParameterValues("keyList");
                if (keys != null) {
                    for (String key : keys) {
                        String value = request.getParameter(key + "Property");
                        if (value != null && !value.isEmpty()) {
                            bundleProperties.put(key, request.getParameter(key + "Property"));
                        }
                    }
                }
                String newkey = request.getParameter("newkey");
                if (!newkey.equals("")) {
                    bundleProperties.put(newkey, request.getParameter("newkeyProperty"));
                }
                bundleConfiguration.setBundleProperties(bundleProperties);

                return new RedirectView(pluginContext.getApplicationPath() + "/edit?id=" + id);
            } catch (Exception e) {
                message = "Error: " + e.getMessage();
                return new RedirectView(pluginContext.getApplicationPath());
            }
        } else if (pluginContext.getLocalPath().equals("/install")) {
            installBundle(request, pluginContext);
            return new RedirectView(pluginContext.getApplicationPath());
        } else if (pluginContext.getLocalPath().equals("/uninstall")) {
            Bundle bundle = getBundleById(request.getParameter("id"));
            if (bundle != null) {
                try {
                    message = "Info: " + bundle.getSymbolicName() + " uninstalled";
                    bundle.uninstall();
                } catch (BundleException e) {
                    message = "Error: " + e.getMessage();
                }
            }
            return new RedirectView(pluginContext.getApplicationPath());
        } else if (pluginContext.getLocalPath().equals("/update")) {
            Bundle bundle = getBundleById(request.getParameter("id"));
            if (bundle != null) {
                try {
                    message = "Info: " + bundle.getSymbolicName() + " updated";
                    bundle.update();
                } catch (BundleException e) {
                    message = "Error: " + e.getMessage();
                }
            }
            return new RedirectView(pluginContext.getApplicationPath());
        } else if (pluginContext.getLocalPath().equals("/stop")) {
            Bundle bundle = getBundleById(request.getParameter("id"));
            if (bundle != null) {
                try {
                    message = "Info: " + bundle.getSymbolicName() + " stoped";
                    bundle.stop();
                } catch (BundleException e) {
                    message = "Error: " + e.getMessage();
                }
            }
            return new RedirectView(pluginContext.getApplicationPath());
        } else if (pluginContext.getLocalPath().equals("/start")) {
            Bundle bundle = getBundleById(request.getParameter("id"));
            if (bundle != null) {
                try {
                    message = "Info: " + bundle.getSymbolicName() + " started";
                    bundle.start();
                } catch (BundleException e) {
                    message = "Error: " + e.getMessage();
                }
            }
            return new RedirectView(pluginContext.getApplicationPath());
        } else {
            String temp = message;
            message = null;
            return new BundleListView(context, loader, temp);
        }
    }

    private Bundle getBundleById(String idString) {
        if (idString != null) {
            long id = new Long(idString);
            return context.getBundle(id);
        } else {
            return null;
        }
    }

    private void installBundle(HttpServletRequest request, PluginContext pluginContext) {
        if (ServletFileUpload.isMultipartContent(request)) {
            FileItemFactory factory = new DiskFileItemFactory();
            ServletFileUpload upload = new ServletFileUpload(factory);

            try {
                List<?> files = upload.parseRequest(request);

                byte[] buffer = new byte[8192];
                for (Object name : files) {
                    FileItem element = (FileItem) name;

                    if (!element.isFormField()) {
                        String fileName = element.getName();
                        if (!fileName.endsWith(".jar")) {
                            throw new FileUploadException("Wrong data type. Needs to be a \".jar\".");
                        }
                        fileName = fileName.replace('\\', '/'); // Windows stub
                        if (fileName.contains("/")) {
                            fileName = fileName.substring('/' + 1);
                        }

                        InputStream is = element.getInputStream();
                        FileOutputStream fos = new FileOutputStream(new File("bundle", fileName));

                        int len = 0;
                        while ((len = is.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }

                        fos.flush();
                        fos.close();
                        is.close();

                        context.installBundle("file:bundle/" + fileName);

                        message = "Info: Installed Bundle " + fileName;
                    }
                }
            } catch (Exception e) {
                message = "Error: " + e.getMessage();
            }
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
        return PluginCategory.CONFIGTOOL;
    }

    protected void setMetaTypeService(MetaTypeService service) {
        metaTypeService = service;
    }

    protected void unsetMetaTypeService(MetaTypeService service) {
        metaTypeService = null;
    }

    protected void setConfigurationAdmin(ConfigurationAdmin service) {
        configAdmin = service;
    }

    protected void unsetConfigurationAdmin(ConfigurationAdmin service) {
        configAdmin = null;
    }
}
