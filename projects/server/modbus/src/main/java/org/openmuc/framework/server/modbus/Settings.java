/*
 * Copyright 2011-2021 Fraunhofer ISE
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
package org.openmuc.framework.server.modbus;

import org.openmuc.framework.lib.osgi.config.GenericSettings;
import org.openmuc.framework.lib.osgi.config.ServiceProperty;

class Settings extends GenericSettings {
    static final String PORT = "port";
    static final String ADDRESS = "address";
    static final String UNITID = "unitId";
    static final String TYPE = "type";
    static final String POOLSIZE = "poolsize";

    Settings() {
        super();
        properties.put(PORT, new ServiceProperty(PORT, "Port to listen on", "502", false));
        properties.put(ADDRESS, new ServiceProperty(ADDRESS, "IP address to listen on", "127.0.0.1", false));
        properties.put(UNITID, new ServiceProperty(UNITID, "UnitId of the slave", "15", false));
        properties.put(POOLSIZE, new ServiceProperty(POOLSIZE,
                "Listener thread pool size, only has affects with TCP and RTUTCP", "3", false));
        properties.put(TYPE, new ServiceProperty(TYPE, "Connection type, could be TCP, RTUTCP or UDP", "tcp", false));
    }
}
