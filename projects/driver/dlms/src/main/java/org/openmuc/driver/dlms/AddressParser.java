/*
 * Copyright 2011-14 Fraunhofer ISE
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
package org.openmuc.driver.dlms;

import org.openmuc.jdlms.client.ClientConnectionSettings;
import org.openmuc.jdlms.client.ClientConnectionSettings.Authentication;
import org.openmuc.jdlms.client.ClientConnectionSettings.ReferencingMethod;
import org.openmuc.jdlms.client.hdlc.HdlcAddress;
import org.openmuc.jdlms.client.hdlc.HdlcClientConnectionSettings;
import org.openmuc.jdlms.client.ip.TcpClientConnectionSettings;
import org.openmuc.jdlms.client.ip.UdpClientConnectionSettings;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

public class AddressParser {

    private final LegacyDeviceAddressParser legacyParser = new LegacyDeviceAddressParser();

    public ClientConnectionSettings<?> parse(String interfaceAddress,
                                             String deviceAddress,
                                             SettingsHelper settings)
            throws UnknownHostException {
        interfaceAddress.trim();
        if (interfaceAddress == null || interfaceAddress.isEmpty()) {
            return legacyParser.parse(deviceAddress, settings);
        }

        int pos = interfaceAddress.indexOf(":");
        if (pos == -1) {
            throw new IllegalArgumentException("InterfaceAddress must begin with 'protocol:'");
        }

        pos = deviceAddress.indexOf(":");
        if (pos == -1) {
            throw new IllegalArgumentException(
                    "DeviceAddress must have the format 'server-logical-port:client-logical-port'");
        }

        String protocol = interfaceAddress.substring(0, interfaceAddress.indexOf(":"))
                                          .toLowerCase();

        ClientConnectionSettings<?> result = null;

        ReferencingMethod referencing = ReferencingMethod.LN;

        referencing = ReferencingMethod.valueOf(settings.getReferencing());

        if (protocol.equals("hdlc")) {
            result = parseHdlc(interfaceAddress, deviceAddress, referencing, settings);
        } else if (protocol.equals("udp")) {
            result = parseUdp(interfaceAddress, deviceAddress, referencing, settings);
        } else if (protocol.equals("tcp")) {
            result = parseTcp(interfaceAddress, deviceAddress, referencing, settings);
        }

        if (settings.getPassword() != null) {
            result.setAuthentication(Authentication.LOW);
        }

        return result;
    }

    private HdlcClientConnectionSettings parseHdlc(String interfaceAddress,
                                                   String deviceAddress,
                                                   ReferencingMethod referencing,
                                                   SettingsHelper settings) {
        HdlcClientConnectionSettings result = null;

        String[] interfaceTokens = interfaceAddress.split(":");
        String[] deviceTokens = deviceAddress.split(":");

        if (interfaceTokens.length < 2 || interfaceTokens.length > 3) {
            throw new IllegalArgumentException(
                    "InterfaceAddress has unknown format. Use hdlc:port[:serverPhysical] as pattern");
        }
        if (deviceTokens.length != 2) {
            throw new IllegalArgumentException(
                    "DeviceAddress has unknown format. Use serverLogical:clientLogical");
        }

        String port = interfaceTokens[1];
        HdlcAddress client = new HdlcAddress(Integer.parseInt(deviceTokens[1]));
        HdlcAddress server = null;

        if (interfaceTokens.length == 2) {
            server = new HdlcAddress(Integer.parseInt(deviceTokens[0]));
        } else {
            int logical = Integer.parseInt(deviceTokens[0]);
            int physical = Integer.parseInt(interfaceTokens[2]);

            int addressSize = 2;
            if (logical > 127 || physical > 127) {
                addressSize = 4;
            }

            server = new HdlcAddress(logical, physical, addressSize);
        }

        if (client.isValidAddress() == false) {
            throw new IllegalArgumentException("Client logical address must be in range [1, 127]");
        }
        if (server.isValidAddress() == false) {
            throw new IllegalArgumentException("Server address is invalid");
        }

        boolean useHandshake = settings.useHandshake();
        int baudrate = settings.getBaudrate();

        result = new HdlcClientConnectionSettings(port, client, server, referencing).setBaudrate(
                baudrate)
                                                                                    .setUseHandshake(
                                                                                            useHandshake);

        return result;
    }

    private UdpClientConnectionSettings parseUdp(String interfaceAddress,
                                                 String deviceAddress,
                                                 ReferencingMethod referencing,
                                                 SettingsHelper settings)
            throws UnknownHostException {
        UdpClientConnectionSettings result = null;

        String[] interfaceTokens = interfaceAddress.split(":");
        String[] deviceTokens = deviceAddress.split(":");

        if (interfaceTokens.length < 2 && interfaceTokens.length > 3) {
            throw new IllegalArgumentException(
                    "InterfaceAddress has unknown format. Use udp:serverIp[:serverPort] as a pattern");
        }
        if (deviceTokens.length != 2) {
            throw new IllegalArgumentException(
                    "DeviceAddress has unknown format. Use serverWPort:clientWPort");
        }

        int serverPort = 4059;
        if (interfaceTokens.length == 3) {
            serverPort = Integer.parseInt(interfaceTokens[2]);
        }
        int clientWPort = Integer.parseInt(deviceTokens[1]);
        InetSocketAddress serverAddress = new InetSocketAddress(InetAddress.getByName(
                interfaceTokens[1]), serverPort);
        int serverWPort = Integer.parseInt(deviceTokens[0]);

        result = new UdpClientConnectionSettings(serverAddress,
                                                 serverWPort,
                                                 clientWPort,
                                                 referencing);

        return result;
    }

    private TcpClientConnectionSettings parseTcp(String interfaceAddress,
                                                 String deviceAddress,
                                                 ReferencingMethod referencing,
                                                 SettingsHelper settings)
            throws UnknownHostException {
        TcpClientConnectionSettings result = null;

        String[] interfaceTokens = interfaceAddress.split(":");
        String[] deviceTokens = deviceAddress.split(":");

        if (interfaceTokens.length < 2 && interfaceTokens.length > 3) {
            throw new IllegalArgumentException(
                    "InterfaceAddress has unknown format. Use tcp:serverIp[:serverPort] as a pattern");
        }
        if (deviceTokens.length != 2) {
            throw new IllegalArgumentException(
                    "DeviceAddress has unknown format. Use serverWPort:clientWPort");
        }

        int serverPort = 4059;
        if (interfaceTokens.length == 3) {
            serverPort = Integer.parseInt(interfaceTokens[2]);
        }
        int clientWPort = Integer.parseInt(deviceTokens[1]);
        InetSocketAddress serverAddress = new InetSocketAddress(InetAddress.getByName(
                interfaceTokens[1]), serverPort);
        int serverWPort = Integer.parseInt(deviceTokens[0]);

        result = new TcpClientConnectionSettings(serverAddress,
                                                 serverWPort,
                                                 clientWPort,
                                                 referencing);

        return result;
    }
}
