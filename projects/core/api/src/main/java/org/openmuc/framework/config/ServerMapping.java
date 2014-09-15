package org.openmuc.framework.config;

/**
 * Class containing the identifier and the address of a server configuration.
 *
 * @author sfey
 */
public class ServerMapping {
    private final String id;
    private final String serverAddress;

    public ServerMapping(String id, String serverAddress) {
        this.id = id;
        this.serverAddress = serverAddress;
    }

    public String getId() {
        return this.id;
    }

    public String getServerAddress() {
        return this.serverAddress;
    }
}
