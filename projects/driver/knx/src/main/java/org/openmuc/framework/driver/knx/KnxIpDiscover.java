/*
 * Copyright 2011-16 Fraunhofer ISE
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
package org.openmuc.framework.driver.knx;

import java.io.IOException;
import java.net.InetAddress;

import org.openmuc.framework.config.DeviceScanInfo;
import org.openmuc.framework.driver.spi.DriverDeviceScanListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tuwien.auto.calimero.knxnetip.Discoverer;
import tuwien.auto.calimero.knxnetip.servicetype.SearchResponse;

public class KnxIpDiscover {

    private static Logger logger = LoggerFactory.getLogger(KnxIpDiscover.class);
    private static int DEFALUT_TIMEOUT = 5;

    private Discoverer discoverer;
    private SearchResponse[] searchResponses;

    public KnxIpDiscover(String interfaceAddress, boolean natAware, boolean mcastResponse) throws IOException {
        try {
            // System.setProperty("java.net.preferIPv4Stack", "true");
            InetAddress localHost = InetAddress.getByName(interfaceAddress);
            discoverer = new Discoverer(localHost, 0, natAware, mcastResponse);
        } catch (Exception e) {
            logger.warn("can not create discoverer: " + e.getMessage());
            throw new IOException(e);
        }
    }

    public void startSearch(int timeout, DriverDeviceScanListener listener) throws IOException {
        timeout = timeout / 1000;
        if (timeout < 1) {
            timeout = DEFALUT_TIMEOUT;
        }
        try {
            logger.debug("Starting search (timeout: " + timeout + "s)");
            discoverer.startSearch(timeout, true);
            searchResponses = discoverer.getSearchResponses();
        } catch (Exception e) {
            logger.warn("A network I/O error occurred");
            e.printStackTrace();
            throw new IOException(e);
        }
        if (searchResponses != null) {
            notifyListener(listener);
        }
    }

    private void notifyListener(DriverDeviceScanListener listener) {

        for (SearchResponse response : searchResponses) {
            StringBuilder deviceAddress = new StringBuilder();
            deviceAddress.append(KnxDriver.ADDRESS_SCHEME_KNXIP).append("://");
            String ipAddress = response.getControlEndpoint().getAddress().getHostAddress();
            if (ipAddress.contains(":")) { // if it is an ipv6 address
                deviceAddress.append("[").append(ipAddress).append("]");
            }
            else {
                deviceAddress.append(ipAddress);
            }
            deviceAddress.append(":").append(response.getControlEndpoint().getPort());

            String name = response.getDevice().getSerialNumberString();
            String description = response.getDevice().toString();

            logger.debug("Found " + deviceAddress + " - " + name + " - " + description);

            listener.deviceFound(new DeviceScanInfo(deviceAddress.toString(), "", description));
        }

    }
}
