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

import org.osgi.framework.Bundle;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.MetaTypeService;

import java.io.*;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;

public final class BundleConfiguration {

    private Configuration config;
    private boolean hasConfig;
    private final ConfigurationAdmin confAdmin;
    private final Bundle bundle;

    public BundleConfiguration(Bundle bundle, ConfigurationAdmin configAdmin, MetaTypeService meta) throws IOException,
            InvalidSyntaxException {
        hasConfig = false;

        this.bundle = bundle;
        String location = bundle.getLocation();
        confAdmin = configAdmin;
        if (confAdmin != null) {
            Configuration[] configs = confAdmin.listConfigurations(null);
            if (configs != null) {
                for (Configuration config : configs) {
                    if (config.getBundleLocation() != null) {
                        config = confAdmin.getConfiguration(config.getPid());
                        if (config.getBundleLocation().contains(location) || location.contains(config.getBundleLocation())) {
                            this.config = config;
                            hasConfig = true;
                        }
                    }

                }
            }
        }

        if (config == null && meta != null) {
            MetaTypeInformation info = meta.getMetaTypeInformation(bundle);
            String[] pids = info.getPids();

            for (String pid : pids) {
                config = confAdmin.getConfiguration(pid, bundle.getLocation());
            }

        }

    }

    public Dictionary<String, Object> getBundleProperties() {
        if (hasConfig) {
            return config.getProperties();
        } else {
            Dictionary<String, Object> properties = new Hashtable<String, Object>();
            return properties;
        }
    }

    public void setBundleProperties(Dictionary<String, String> bundleProperties) throws IOException {
        if (bundleProperties.size() == 1) {
            config = confAdmin.createFactoryConfiguration(bundle.getSymbolicName());
            config.setBundleLocation(bundle.getLocation());
        }
        if (bundleProperties.size() >= 1) {
            config.update(bundleProperties);

            Writer output = null;

            String text = new String();
            Enumeration<String> keys = bundleProperties.keys();
            while (keys.hasMoreElements()) {
                String key = keys.nextElement();
                if (!key.contains("service.")) {
                    text = text + key + "=" + bundleProperties.get(key) + "\n";
                }
            }

            File file = null;
            String path = System.getProperty("bundles.configuration.location");
            if (path != null) {
                path.trim();
                if (path.endsWith("/")) {
                    path = path.substring(0, path.length() - 1);
                }
                file = new File(path + "/services/" + config.getPid() + ".properties");
            } else {
                file = new File("configurations/services/" + config.getPid() + ".properties");
            }
            if (file.exists()) { // only write if paxconfman data available
                output = new BufferedWriter(new FileWriter(file));
                output.write(text);
                output.flush();
                output.close();
            }
        } else {
            try {
                config.delete();
            } catch (Exception e) {
            }
        }
    }

    public boolean hasConfig() {
        return hasConfig;
    }

    public Configuration getConfig() {
        return config;
    }

}
