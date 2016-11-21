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

import org.openmuc.framework.config.ArgumentSyntaxException;
import org.openmuc.framework.config.DriverInfo;
import org.openmuc.framework.config.ScanException;
import org.openmuc.framework.config.ScanInterruptedException;
import org.openmuc.framework.driver.spi.Connection;
import org.openmuc.framework.driver.spi.ConnectionException;
import org.openmuc.framework.driver.spi.DriverDeviceScanListener;
import org.openmuc.framework.driver.spi.DriverService;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tuwien.auto.calimero.log.LogManager;

@Component
public class KnxDriver implements DriverService {

    public static final String ADDRESS_SCHEME_KNXIP = "knxip";
    private static Logger logger = LoggerFactory.getLogger(KnxDriver.class);

    final static int timeout = 10000;

    private final static DriverInfo info = new DriverInfo(
            // id*/
            "knx",
            // description
            "Driver to read and write KNX groupaddresses.",
            // device address
            ADDRESS_SCHEME_KNXIP + "://<host_ip>[:<port>];" + ADDRESS_SCHEME_KNXIP + "://<device_ip>[:<port>]",
            // settings
            "[Address=<Individual KNX address (e. g. 2.6.52)>];[SerialNumber=<Serial number>]",
            // channel address
            "<Group_Adress>:<DPT_ID>",
            // device scan settings
            "<host_ip>;<mcast> for multicast scan or <host_ip>;<nat> for NAT scan");

    protected void activate(ComponentContext context) {
        if (logger.isDebugEnabled()) {
            LogManager.getManager().addWriter("", new KnxLogWriter()); // Add calimero logger
        }
    }

    protected void deactivate(ComponentContext context) {
        logger.debug("Deactivated KNX Driver");
    }

    @Override
    public DriverInfo getInfo() {
        return info;
    }

    @Override
    public void scanForDevices(String settings, DriverDeviceScanListener listener)
            throws UnsupportedOperationException, ArgumentSyntaxException, ScanException, ScanInterruptedException {

        String[] args = null;
        logger.debug("settings = " + settings);
        if (settings != null && !settings.isEmpty() && settings.length() == 2) {
            args = settings.split(";");
            logger.debug("args[0] = " + args[0]);
            logger.debug("args[1] = " + args[1]);
        }

        if (args != null && args.length > 0) {
            boolean natAware = false;
            boolean mcastResponse = false;
            if (args.length > 1) {
                logger.debug("Applying settings: " + args[1]);
                natAware = args[1].contains("nat");
                mcastResponse = args[1].contains("mcast");
            }
            KnxIpDiscover discover;
            try {
                discover = new KnxIpDiscover(args[0], natAware, mcastResponse);
                discover.startSearch(0, listener);
            } catch (IOException e) {
                throw new ScanException(e);
            }
        }
    }

    @Override
    public void interruptDeviceScan() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();

    }

    @Override
    public Connection connect(String deviceAddress, String settings)
            throws ArgumentSyntaxException, ConnectionException {
        return new KnxConnection(deviceAddress, settings, timeout);
    }

}
