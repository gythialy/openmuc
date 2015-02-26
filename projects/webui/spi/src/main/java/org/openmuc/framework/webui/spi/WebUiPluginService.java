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

package org.openmuc.framework.webui.spi;

import org.osgi.service.http.HttpContext;

import javax.servlet.http.HttpServletRequest;
import java.util.Hashtable;

public interface WebUiPluginService extends HttpContext {

    /**
     * @return Name of WebUI-Plugin, displayed in OpenMUC main menu on top
     */
    public String getName();

    /**
     * @return Alias of the WebUI-Plugin. The Alias is the identifier in the URL.
     */
    public String getAlias();

    /**
     * @return This string is displayed in the overview of each category.
     */
    public String getDescription();

    /**
     * @return Assigns WebUI-Plugin to OpenMUC webui's categories in main menu
     */
    public PluginCategory getCategory();

    /**
     * Function is called when a HTTP request arrives at the WebUI-Plugin.
     * /openmuc/applications/&lt;Plugin.getAlias&gt;XXX -&gt; XXX is part of context parameter.
     *
     * @param request properties of HTTP request
     * @param context content of HTTP request
     * @return view View contains the HTTP response and several properties
     */
    public View getContentView(HttpServletRequest request, PluginContext context);

    /**
     * add additional resources if needed
     *
     * @return the resources as a hash table.
     */
    public Hashtable<String, String> getResources();
}
