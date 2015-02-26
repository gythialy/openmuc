/*
 * Copyright 2011-15 Fraunhofer ISE
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
package org.openmuc.framework.server.asciisocket;

import org.openmuc.framework.dataaccess.DataAccessService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Vector;

/**
 * Simple ASCII based socket server for access to measured data. The protocol is kept simple so it can be used my humans
 * via telnet.
 * <p/>
 * The IP and port the server will listen can be configured by the OSGi/Java properties "org.openmuc.mux.dataserver.ip"
 * and "org.openmuc.mux.dataserver.port".
 * <p/>
 * The default port is 9200. If no IP address is configuered the server will listen to all IPs of the system.
 */
public class AsciiSocketServer extends Thread {
    private static Logger logger = LoggerFactory.getLogger(AsciiSocketServer.class);

    public static int STATE_UNBOUND = 0;
    public static int STATE_LISTEN = 1;
    public static int STATE_ERROR = 2;

    private int serverState = STATE_UNBOUND;

    private ServerSocket serverSocket;

    private final Vector<Connection> connectionList = new Vector<Connection>();

    DataAccessService dataAccessService;

    protected void activate(ComponentContext context) throws IOException {
        logger.info("Activating Simple Socket Server");
        start();
    }

    protected void deactivate(ComponentContext context) {
        logger.info("Deactivating Simple Socket Server");

    }

    protected void setDataAccessService(DataAccessService dataAccessService) {
        this.dataAccessService = dataAccessService;
    }

    protected void unsetDataAccessService(DataAccessService dataAccessService) {
        dataAccessService = null;
    }

    /**
     * Get the state of the server.
     *
     * @return state one of STATE_UNBOUND, STATE_LISTEN or STATE_ERROR
     */
    public int getServerState() {
        return serverState;
    }

    /**
     * The main server thread.
     */
    @Override
    public void run() {

        try {
            String ipAddrStr = System.getProperty("org.openmuc.framework.server.simplesocket.ip");
            String portStr = System.getProperty("org.openmuc.framework.server.simplesocket.port");

            int port;
            if (portStr != null) {
                port = new Integer(portStr).intValue();
            } else {
                port = 9200;
            }

            if (ipAddrStr != null) {
                InetAddress inetAddr = InetAddress.getByName(ipAddrStr);
                serverSocket = new ServerSocket(port, 0, inetAddr);
            } else {
                serverSocket = new ServerSocket(port);
            }

            Socket clientSock;

            logger.info("ASCII Socket Server listening  on port: " + port);

            try {
                while ((clientSock = serverSocket.accept()) != null) {
                    Connection con = new Connection(dataAccessService, clientSock, this);
                    connectionList.add(con);
                    con.start();
                }
            } catch (SocketException e) {

            }
            logger.info("DataSocketServer stopped.");

        } catch (IOException e) {
            e.printStackTrace();
            serverState = STATE_ERROR;
        }

        logger.info("DataSocketServer thread terminated!");
    }

    /**
     * Callback method to be called by Connection objects if the connection is closed by timeout or on clients request.
     *
     * @param con The calling Connection object
     */
    protected void connectionClosed(Connection con) {
        connectionList.removeElement(con);
    }

    /**
     * Stop the server. Close all client sockets. Called by bundle activator.
     */
    protected void stopServer() {
        logger.info("Close all open sockets");

        for (Connection con : connectionList) {
            con.close();
        }

        logger.info("Stop server.");
        try {
            serverSocket.close();
        } catch (IOException e) {
            logger.error(e.getMessage());
        }

        // this.interrupt();
    }

}
