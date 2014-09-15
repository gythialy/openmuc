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

public class LegacyDeviceAddressParser {

    public ClientConnectionSettings<?> parse(String deviceAddress, SettingsHelper settings)
            throws UnknownHostException {
        int pos = deviceAddress.indexOf(":");
        if (pos == -1) {
            throw new IllegalArgumentException("DeviceAddress must begin with 'protocol:'");
        }

        String protocol = deviceAddress.substring(0, pos).toLowerCase();
        String address = deviceAddress.substring(pos + 1);

        ClientConnectionSettings<?> result = null;

        ReferencingMethod referencing = ReferencingMethod.LN;

        referencing = ReferencingMethod.valueOf(settings.getReferencing());

        if (protocol.equals("hdlc")) {
            result = parseHdlc(address, referencing, settings);
        } else if (protocol.equals("udp")) {
            result = parseUdp(address, referencing, settings);
        } else if (protocol.equals("tcp")) {
            result = parseTcp(address, referencing, settings);
        }

        if (settings.getPassword() != null) {
            result.setAuthentication(Authentication.LOW);
        }

        return result;
    }

    private HdlcClientConnectionSettings parseHdlc(String address, ReferencingMethod referencing,
                                                   SettingsHelper settings) {
        HdlcClientConnectionSettings result = null;

        String[] addresses = address.split(":");

        if (addresses.length < 3 || addresses.length > 4) {
            throw new IllegalArgumentException(
                    "DeviceAddress has unknown format. Use hdlc:port:clientLogical:serverLogical:serverPhysical as pattern");
        }

        String port = addresses[0];
        HdlcAddress client = new HdlcAddress(Integer.parseInt(addresses[1]));
        HdlcAddress server = null;

        if (addresses.length == 3) {
            server = new HdlcAddress(Integer.parseInt(addresses[2]));
        } else {
            int logical = Integer.parseInt(addresses[2]);
            int physical = Integer.parseInt(addresses[3]);

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

    private UdpClientConnectionSettings parseUdp(String address,
                                                 ReferencingMethod referencing,
                                                 SettingsHelper settings)
            throws UnknownHostException {
        UdpClientConnectionSettings result = null;

        String[] addresses = address.split(":");

        if (addresses.length != 4) {
            throw new IllegalArgumentException(
                    "DeviceAddress has unknown format. Use udp:clientwport:serverip:serverport:serverwport as a pattern");
        }

        int clientWPort = Integer.parseInt(addresses[0]);
        InetSocketAddress serverAddress = new InetSocketAddress(InetAddress.getByName(addresses[1]),
                                                                Integer.parseInt(addresses[2]));
        int serverWPort = Integer.parseInt(addresses[3]);

        result = new UdpClientConnectionSettings(serverAddress,
                                                 serverWPort,
                                                 clientWPort,
                                                 referencing);

        return result;
    }

    private TcpClientConnectionSettings parseTcp(String address,
                                                 ReferencingMethod referencing,
                                                 SettingsHelper settings)
            throws UnknownHostException {
        TcpClientConnectionSettings result = null;

        String[] addresses = address.split(":");

        if (addresses.length != 4) {
            throw new IllegalArgumentException(
                    "DeviceAddress has unknown format. Use udp:clientwport:serverip:serverport:serverwport as a pattern");
        }

        int clientWPort = Integer.parseInt(addresses[0]);
        InetSocketAddress serverAddress = new InetSocketAddress(InetAddress.getByName(addresses[1]),
                                                                Integer.parseInt(addresses[2]));
        int serverWPort = Integer.parseInt(addresses[3]);

        result = new TcpClientConnectionSettings(serverAddress,
                                                 serverWPort,
                                                 clientWPort,
                                                 referencing);

        return result;
    }
}
