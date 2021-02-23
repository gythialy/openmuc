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

package org.openmuc.framework.driver.iec61850;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;

import com.beanit.iec61850bean.SclParseException;
import com.beanit.iec61850bean.SclParser;
import com.beanit.iec61850bean.ServerEventListener;
import com.beanit.iec61850bean.ServerModel;
import com.beanit.iec61850bean.ServerSap;

public class TestHelper {

    private static int MIN_PORT_NUMBER = 50000;
    private static int PORT_SCOPE = 10000;

    static int getAvailablePort() {
        int port = MIN_PORT_NUMBER;
        boolean isAvailable = false;

        while (!isAvailable) {
            port = (int) (Math.random() * PORT_SCOPE) + MIN_PORT_NUMBER;
            ServerSocket ss = null;
            DatagramSocket ds = null;

            try {
                ss = new ServerSocket(port);
                ss.setReuseAddress(true);
                ds = new DatagramSocket(port);
                ds.setReuseAddress(true);
                isAvailable = true;
            } catch (IOException e) {
                isAvailable = false;
            } finally {
                if (ds != null) {
                    ds.close();
                }

                if (ss != null) {
                    try {
                        ss.close();
                    } catch (IOException e) {
                        /* should not be thrown */
                    }
                }
            }
        }
        return port;
    }

    static ServerSap runServer(String sclFilePath, int port, ServerSap serverSap, ServerModel serversServerModel,
            ServerEventListener eventListener) throws SclParseException, IOException {

        serverSap = new ServerSap(port, 0, null, SclParser.parse(sclFilePath).get(0), null);
        serverSap.setPort(port);
        serverSap.startListening(eventListener);
        serversServerModel = serverSap.getModelCopy();
        return serverSap;
    }

}
