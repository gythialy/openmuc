package org.openmuc.framework.server.spi;

import java.util.List;

/**
 * This interface is to be implemented by bundles that provide server functionality.
 *
 * @author sfey
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
     */
    public void updatedConfiguration(List<ServerMappingContainer> mappings);

    /**
     * This method is called after registering as a server. It provides access to the channels that are configured to be
     * mapped on a server
     *
     * @param mappings
     */
    public void serverMappings(List<ServerMappingContainer> mappings);
}
