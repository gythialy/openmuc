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

package org.openmuc.framework.webui.spi;

import java.util.Hashtable;

import org.osgi.framework.Bundle;

public interface WebUiPluginService {

    /**
     * @return Name of WebUI-Plugin, displayed in OpenMUC main menu on top
     */
    public String getName();

    /**
     * @return Alias of the WebUI-Plugin. The Alias is the identifier in the URL.
     */
    public String getAlias();

    /**
     * add additional resources if needed
     * 
     * @return the resources as a hash table.
     */
    public Hashtable<String, String> getResources();

    public Bundle getContextBundle();
}
