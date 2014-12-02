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
     *
     * @param mappings the channels configured be mapped to the server
     */
    public void updatedConfiguration(List<ServerMappingContainer> mappings);

    /**
     * This method is called after registering as a server. It provides access to the channels that are configured to be
     * mapped to a server
     *
     * @param mappings the channels configured be mapped to the server
     */
    public void serverMappings(List<ServerMappingContainer> mappings);
}
