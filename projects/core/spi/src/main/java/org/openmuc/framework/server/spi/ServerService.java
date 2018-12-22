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
package org.openmuc.framework.server.spi;

import java.util.List;

/**
 * This interface is to be implemented by bundles that provide server functionality.
 */
public interface ServerService {

    /**
     * returns the unique Identifier of a server
     * 
     * @return the unique Identifier
     */
    public String getId();

    /**
     * This method is called when configuration is updated.
     * 
     * @param mappings
     *            the channels configured be mapped to the server
     */
    public void updatedConfiguration(List<ServerMappingContainer> mappings);

    /**
     * This method is called after registering as a server. It provides access to the channels that are configured to be
     * mapped to a server
     * 
     * @param mappings
     *            the channels configured be mapped to the server
     */
    public void serverMappings(List<ServerMappingContainer> mappings);
}
