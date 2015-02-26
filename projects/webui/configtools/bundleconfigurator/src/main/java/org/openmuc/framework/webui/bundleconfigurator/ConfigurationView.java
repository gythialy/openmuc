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

import org.openmuc.framework.webui.spi.ResourceLoader;
import org.openmuc.framework.webui.spi.View;
import org.osgi.framework.Bundle;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.MetaTypeService;
import org.osgi.service.metatype.ObjectClassDefinition;

import java.util.*;

public final class ConfigurationView implements View {

    private final String template;
    private HashMap<String, Object> context = null;
    private final Bundle bundle;

    public ConfigurationView(Bundle bundle, BundleConfiguration bundleConfiguration, MetaTypeService meta, ResourceLoader loader) {
        template = loader.getResourceAsString("edit.html");
        context = new HashMap<String, Object>();
        this.bundle = bundle;

        Dictionary<String, Object> bundleProperties = bundleConfiguration.getBundleProperties();

        context.put("bundle", bundle);

        HashMap<String, HashMap<String, Object>> map = new HashMap<String, HashMap<String, Object>>();
        if (bundleProperties != null) {
            Enumeration<String> keys = bundleProperties.keys();
            while (keys.hasMoreElements()) {
                HashMap<String, Object> temp = new HashMap<String, Object>();
                String key = keys.nextElement();
                temp.put("value", bundleProperties.get(key));
                temp.put("description", "");
                map.put(key, temp);

            }

            if (meta == null) {
                System.out.println("No MetaTypeService found!");
            } else {
                MetaTypeInformation info = meta.getMetaTypeInformation(bundle);
                String[] pids = info.getPids();

                for (String pid : pids) {
                    ObjectClassDefinition ocd = info.getObjectClassDefinition(pid, null);

                    AttributeDefinition[] ads = ocd.getAttributeDefinitions(ObjectClassDefinition.ALL);
                    for (AttributeDefinition ad : ads) {
                        HashMap<String, Object> temp = new HashMap<String, Object>();

                        temp.put("value", bundleProperties.get(ad.getID()));
                        temp.put("description", ad.getDescription());

                        map.put(ad.getID(), temp);
                    }
                }
            }
        }

        context.put("bundleProperties", map);
    }

    @Override
    public HashMap<String, Object> getContext() {
        return context;
    }

    @Override
    public List<String> getJavaScripts() {
        return new ArrayList<String>();
    }

    @Override
    public List<String> getStyleSheets() {
        ArrayList<String> styleSheets = new ArrayList<String>();
        styleSheets.add("/openmuc/css/openmuc-theme/jquery-ui-1.8.14.custom.css");
        return styleSheets;
    }

    @Override
    public String getTemplate() {
        return template;
    }

    @Override
    public String getPage() {
        return "Configure bundle " + bundle.getSymbolicName();
    }

    @Override
    public String getRedirectLocation() {
        return null;
    }

    @Override
    public viewtype getViewType() {
        // TODO Auto-generated method stub
        return null;
    }
}
