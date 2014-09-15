package org.openmuc.framework.server.spi;

import org.openmuc.framework.config.ServerMapping;
import org.openmuc.framework.dataaccess.Channel;

/**
 * Class that contains the mapping between a server-address/configuration and channel.
 *
 * @author sfey
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
