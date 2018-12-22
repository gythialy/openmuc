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

import org.openmuc.framework.config.ServerMapping;
import org.openmuc.framework.dataaccess.Channel;

/**
 * Class that contains the mapping between a server-address/configuration and channel.
 * 
 * @author sfey
 *
 */
public class ServerMappingContainer {
    private final Channel channel;
    private final ServerMapping serverMapping;

    public ServerMappingContainer(Channel channel, ServerMapping serverMapping) {
        this.channel = channel;
        this.serverMapping = serverMapping;
    }

    /**
     * The serverMapping that the channel should be mapped to.
     * 
     * @return the serverAddress
     */
    public ServerMapping getServerMapping() {
        return this.serverMapping;
    }

    /**
     * The mapped Channel
     * 
     * @return the channel
     */
    public Channel getChannel() {
        return this.channel;
    }
}
