package org.openmuc.framework.driver.modbus.rtutcp.bonino;

/**
 * A common interface for master connections (not strictly covering serial connections)
 * 
 * @author bonino
 * 
 *         https://github.com/dog-gateway/jamod-rtu-over-tcp
 */

public interface MasterConnection {

    public void connect() throws Exception;

    public boolean isConnected();

    public void close();

}
