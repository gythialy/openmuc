/*
 * Copyright 2011-2022 Fraunhofer ISE
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

package org.openmuc.framework.lib.osgi.config;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Should be inherited by your own settings class.<br>
 * <b>Example:</b>
 *
 * <pre>
 * public class Settings extends GenericSettings {
 *
 *     public static final String PORT = "port";
 *     public static final String HOST = "host";
 *
 *     public Settings() {
 *         super();
 *         properties.put(PORT, new ServiceProperty(PORT, "port for communication", "1234", true));
 *         properties.put(HOST, new ServiceProperty(HOST, "URL of service", "localhost", true));
 *     }
 *
 * }
 * </pre>
 */
public class GenericSettings {

    protected Map<String, ServiceProperty> properties;

    protected GenericSettings() {
        properties = new LinkedHashMap<>();
    }

    public Map<String, ServiceProperty> getProperties() {
        return properties;
    }
}
