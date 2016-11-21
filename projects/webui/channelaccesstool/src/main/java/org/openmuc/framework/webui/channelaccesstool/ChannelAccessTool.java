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
package org.openmuc.framework.webui.channelaccesstool;

import java.util.Hashtable;

import org.openmuc.framework.webui.spi.WebUiPluginService;
import org.osgi.framework.Bundle;
import org.osgi.service.component.ComponentContext;

public final class ChannelAccessTool implements WebUiPluginService {

    private Bundle bundle;

    protected void activate(ComponentContext context) {
        bundle = context.getBundleContext().getBundle();
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
    public Hashtable<String, String> getResources() {
        Hashtable<String, String> resources = new Hashtable<>();

        resources.put("html", "html");
        resources.put("css", "css");
        resources.put("js", "js");
        resources.put("images", "images");

        return resources;
    }

    @Override
    public Bundle getContextBundle() {
        return bundle;
    }

}
