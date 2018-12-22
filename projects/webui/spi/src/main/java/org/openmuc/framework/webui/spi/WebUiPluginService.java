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

package org.openmuc.framework.webui.spi;

import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;

public abstract class WebUiPluginService {

    private Bundle contextBundle;

    @Activate
    protected void activate(ComponentContext context) {
        contextBundle = context.getBundleContext().getBundle();
    }

    public Bundle getContextBundle() {
        return contextBundle;
    }

    /**
     * @return Name of WebUI-Plugin, displayed in OpenMUC main menu on top
     */
    public abstract String getName();

    /**
     * @return Alias of the WebUI-Plugin. The Alias is the identifier in the URL.
     */
    public abstract String getAlias();

    /**
     * add additional resources if needed
     * 
     * @return the resources as a hash table.
     */
    public Map<String, String> getResources() {
        HashMap<String, String> resources = new HashMap<>();

        resources.put("html", "html");
        resources.put("css", "css");
        resources.put("js", "js");
        resources.put("images", "images");

        return resources;
    }

}
