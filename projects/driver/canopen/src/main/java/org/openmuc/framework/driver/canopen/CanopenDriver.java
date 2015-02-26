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
package org.openmuc.framework.driver.canopen;

import org.openmuc.framework.config.*;
import org.openmuc.framework.driver.spi.Connection;
import org.openmuc.framework.driver.spi.ConnectionException;
import org.openmuc.framework.driver.spi.DriverDeviceScanListener;
import org.openmuc.framework.driver.spi.DriverService;
import org.openmuc.jcanopen.exc.CanException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Frederic Robra
 */
public class CanopenDriver implements DriverService {

    private static Logger logger = LoggerFactory.getLogger(CanopenDriver.class);

    static final DriverInfo driverInfo = new DriverInfo("canopen", // id
                                                        // description
                                                        "Generic driver for CANopen",
                                                        // device address
                                                        "Name of the interface. e.g. \"can0\"",
                                                        // parameters
                                                        "[NMT];[SYNC=<time ms>]\n" + "NMT: The driver controls the CAN network e.g. " +
                                                                "starts and stops nodes.\n" + "SYNC: The driver sends a sync package, may" +
                                                                " be neccessary to receive PDOs",
                                                        // channel address
                                                        "SDO:<CAN ID>:<Object Index>:<Object Subindex>[:<Data Type>]\n" //
                                                                + "\te.g. SDO:0x1:0x7130:1:INTEGER16\n\n" + "PDO:<PDO " +
                                                                "ID>:<Position>:<Length>[:<Data Type>]\n" + "\te.g. " +
                                                                "PDO:0x181:0:16:INTEGER16\n\n" + "Data Type: <UNSIGNED8|UNSIGNED16|.." +
                                                                ".|INTEGER8|...|REAL32|REAL64>\n" + "PDO ID: The COB ID of the PDO " +
                                                                "message\n" + "Position: PDOs with the same ID are sorted by the position" +
                                                                " and aligned by the length\n" + "Length: The length, in bit, of the " +
                                                                "specified data in this PDO\n" + "IDs and numbers are either decimal (42)" +
                                                                " or hex (0x2A)",
                                                        // device scan parameters
                                                        "N.A.");

    @Override
    public DriverInfo getInfo() {
        return driverInfo;
    }

    @Override
    public void scanForDevices(String settings, DriverDeviceScanListener listener) throws UnsupportedOperationException,
            ArgumentSyntaxException, ScanException, ScanInterruptedException {
        if (!System.getProperty("os.name").equals("Linux")) {
            throw new UnsupportedOperationException();
        }
        try {
            Pattern pattern = Pattern.compile("\\w*can\\w*");
            FileReader reader = new FileReader("/proc/net/dev");
            BufferedReader in = new BufferedReader(reader);
            String line = null;
            while ((line = in.readLine()) != null) {
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    listener.deviceFound(new DeviceScanInfo(matcher.group(), null, null));
                }
            }
            in.close();
            reader.close();
        } catch (IOException e) {
            logger.warn(e.getMessage());
        }
    }

    @Override
    public void interruptDeviceScan() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Connection connect(String deviceAddress, String settings) throws ArgumentSyntaxException, ConnectionException {
        logger.info("connecting to {}", deviceAddress);
        CanopenConnection connection = new CanopenConnection(deviceAddress);
        if (settings != null) {
            String[] configs = settings.split(";");
            for (String config : configs) {
                if (config.equals("NMT")) {
                    try {
                        connection.startNMT();
                    } catch (CanException e) {
                        throw new ConnectionException();
                    }
                } else if (config.startsWith("SYNC")) {
                    try {
                        long time = Long.parseLong(config.split("=")[1]);
                        connection.startSync(time);
                    } catch (Exception e) {
                        logger.warn("SYNC parameter wrong: {}" + config);
                    }
                }
            }
        }
        return connection;
    }

}
