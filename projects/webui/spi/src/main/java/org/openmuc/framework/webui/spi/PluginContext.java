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

package org.openmuc.framework.webui.spi;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface PluginContext {

    /**
     * Get the path of plugin application including the application alias. The path is the path relative to the
     * application containers root. This should be used for absolute links to local resources. E.g.
     * "/openmuc/configtool/channelconfigurator" from
     * "http://127.0.0.1:8888/openmuc/configtool/channelconfigurator/devices#dummy"
     *
     * @return the plugins applications base path
     */
    public String getApplicationPath();

    /**
     * Get the plugin applications local path. This is part of the servlets PathInfo without the servlet context, plugin
     * category and application alias. E.g. "/devices" from
     * "http://127.0.0.1:8888/openmuc/configtool/channelconfigurator/devices#dummy"
     *
     * @return the plug-ins local part of PathInfo
     */
    public String getLocalPath();

    /**
     * Get the plugin alias path. E.g. "channelconfigurator" from
     * "http://127.0.0.1:8888/openmuc/configtool/channelconfigurator/devices#dummy"
     *
     * @return plugin alias
     */
    public String getApplicationAlias();

    public HttpServletResponse getResponse();

    public HttpServletRequest getRequest();
}
