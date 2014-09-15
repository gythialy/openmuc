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
package org.openmuc.framework.driver.canopen;

import org.openmuc.framework.config.*;
import org.openmuc.framework.data.Flag;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.driver.spi.*;
import org.openmuc.jcanopen.exc.CanException;
import org.openmuc.jcanopen.pdo.PDOMapping;
import org.openmuc.jcanopen.pdo.PDOObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Frederic Robra
 */
public class CanopenDriver implements DriverService {

    private static Logger logger = LoggerFactory.getLogger(CanopenDriver.class);
    private static final long DEFAULT_TIMEOUT = 5000;
    private static final DriverInfo driverInfo = new DriverInfo("canopen",
                                                                // driverId
                                                                "Generic driver for CANopen",
                                                                // description
                                                                "N.A.",
                                                                // interfaceAddressSyntax
                                                                "Name of the interface. e.g. \"can0\"",
                                                                // deviceAddressSyntax
                                                                "[NMT];[SYNC=<time ms>]\n" //
                                                                + "NMT: The driver controls the CAN network e.g. starts and stops nodes.\n"
                                                                + "SYNC: The driver sends a sync package, may be neccessary to receive PDOs",
                                                                // parametersSyntax
                                                                "SDO:<CAN ID>:<Object Index>:<Object Subindex>[:<Data Type>]\n"
                                                                //
                                                                + "\te.g. SDO:0x1:0x7130:1:INTEGER16\n\n"
                                                                + "PDO:<PDO ID>:<Position>:<Length>[:<Data Type>]\n"
                                                                + "\te.g. PDO:0x181:0:16:INTEGER16\n\n"
                                                                + "Data Type: <UNSIGNED8|UNSIGNED16|...|INTEGER8|...|REAL32|REAL64>\n"
                                                                + "PDO ID: The COB ID of the PDO message\n"
                                                                + "Position: PDOs with the same ID are sorted by the position and aligned by the length\n"
                                                                + "Length: The length, in bit, of the specified data in this PDO\n"
                                                                + "IDs and numbers are either decimal (42) or hex (0x2A)",
                                                                // channelAddressSyntax
                                                                "N.A."
                                                                // deviceScanParametersSyntax
    );

    @Override
    public DriverInfo getInfo() {
        return driverInfo;
    }

    @Override
    public void scanForDevices(String settings, DriverDeviceScanListener listener)
            throws UnsupportedOperationException, ArgumentSyntaxException, ScanException,
            ScanInterruptedException {
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
                    listener.deviceFound(new DeviceScanInfo(null, matcher.group(), null, null));
                }
            }
            in.close();
            reader.close();
        }
        catch (IOException e) {
            logger.warn(e.getMessage());
        }
    }

    @Override
    public void interruptDeviceScan() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ChannelScanInfo> scanForChannels(DeviceConnection connection, String settings)
            throws UnsupportedOperationException, ArgumentSyntaxException, ScanException,
            ConnectionException {
        CanopenConnection canConnection = (CanopenConnection) connection.getConnectionHandle();

        List<ChannelScanInfo> infos = new LinkedList<ChannelScanInfo>();
        ExecutorService executor = Executors.newCachedThreadPool();
        Collection<ChannelScanner> tasks = new LinkedList<ChannelScanner>();
        int steps = 10;
        for (int i = 1; i < 256; i += steps) {
            tasks.add(new ChannelScanner(i, i + steps, canConnection));
        }
        try {
            List<Future<List<ChannelScanInfo>>> results = executor.invokeAll(tasks);

            for (Future<List<ChannelScanInfo>> result : results) {
                infos.addAll(result.get());
            }

        }
        catch (Exception e) {
            logger.warn("failed to scan channels: {}", e.getMessage());
        }

        return infos;
    }

    @Override
    public Object connect(String interfaceAddress, String deviceAddress, String settings)
            throws ArgumentSyntaxException, ConnectionException {
        logger.info("connecting to {}", deviceAddress);
        CanopenConnection connection = new CanopenConnection(deviceAddress);
        if (settings != null) {
            String[] configs = settings.split(";");
            for (String config : configs) {
                if (config.equals("NMT")) {
                    try {
                        connection.startNMT();
                    }
                    catch (CanException e) {
                        throw new ConnectionException();
                    }
                } else if (config.startsWith("SYNC")) {
                    try {
                        long time = Long.parseLong(config.split("=")[1]);
                        connection.startSync(time);
                    }
                    catch (Exception e) {
                        logger.warn("SYNC parameter wrong: {}" + config);
                    }
                }
            }
        }
        return connection;
    }

    @Override
    public void disconnect(DeviceConnection connection) {
        logger.info("disconnecting");
        ((CanopenConnection) connection.getConnectionHandle()).close();
    }

    @Override
    public Object read(DeviceConnection connection, List<ChannelRecordContainer> containers,
                       Object containerListHandle, String samplingGroup)
            throws UnsupportedOperationException, ConnectionException {

        CanopenConnection canConnection = (CanopenConnection) connection.getConnectionHandle();
        for (ChannelRecordContainer container : containers) {
            try {
                SDOObject sdoObject = null;
                if (container.getChannelHandle() == null) {
                    sdoObject = new SDOObject(container.getChannelAddress());
                    container.setChannelHandle(sdoObject);
                } else {
                    sdoObject = (SDOObject) container.getChannelHandle();
                }
                Record record = canConnection.readSDO(sdoObject, DEFAULT_TIMEOUT);
                container.setRecord(record);
            }
            catch (ArgumentSyntaxException e) {
                logger.warn("read failed: channel address syntax invalid: {}",
                            container.getChannelAddress());
                Record record = new Record(null, System.currentTimeMillis(),
                                           Flag.DRIVER_ERROR_CHANNEL_ADDRESS_SYNTAX_INVALID);
                container.setRecord(record);
            }
            catch (CanException e) {
                logger.warn("read failed: {}", e.getMessage());
                Record record = new Record(null, System.currentTimeMillis(), Flag.UNKNOWN_ERROR);
                container.setRecord(record);
            }
        }

        return null;
    }

    @Override
    public void startListening(DeviceConnection connection, List<ChannelRecordContainer> containers,
                               RecordsReceivedListener listener)
            throws UnsupportedOperationException, ConnectionException {

        CanopenConnection canConnection = (CanopenConnection) connection.getConnectionHandle();

		/*
         * create a map with the COB ID of this PDO and a list of objects
		 */
        Map<Integer, List<PDOObjectImpl>> datas = new HashMap<Integer, List<PDOObjectImpl>>();
        List<ChannelRecordContainer> errors = new LinkedList<ChannelRecordContainer>();
        for (ChannelRecordContainer container : containers) {
            try {
                PDOObjectImpl pdoObject = new PDOObjectImpl(container);
                int cobId = pdoObject.getCobId();

                logger.trace("add pdo {} with Id {}", container.getChannelAddress(), cobId);
                List<PDOObjectImpl> list = null;
                if (!datas.containsKey(cobId)) {
                    list = new LinkedList<PDOObjectImpl>();
                } else {
                    list = datas.get(cobId);
                }
                list.add(pdoObject);
                datas.put(cobId, list);
            }
            catch (ArgumentSyntaxException e) {
                logger.warn("channel address syntax invalid {}", container.getChannelAddress());
                Record record = new Record(null, System.currentTimeMillis(),
                                           Flag.DRIVER_ERROR_CHANNEL_ADDRESS_SYNTAX_INVALID);
                container.setRecord(record);
                errors.add(container);
            }
        }
        listener.newRecords(errors);

		/*
		 * sort the list of objects for each PDO by its position and create a PDO mapping.
		 */
        PDOMapping[] mappings = new PDOMapping[datas.size()];
        Iterator<Entry<Integer, List<PDOObjectImpl>>> entries = datas.entrySet().iterator();
        for (int i = 0; entries.hasNext(); i++) {
            Entry<Integer, List<PDOObjectImpl>> entry = entries.next();
            List<PDOObjectImpl> list = entry.getValue();
            Collections.sort(list);
            PDOObject[] objects = new PDOObject[list.size()];

            for (int j = 0; j < list.size(); j++) {
                objects[j] = list.get(j);
            }

            logger.trace("add pdo mapping {}", entry.getKey());
            mappings[i] = new PDOMapping(entry.getKey(), objects);
        }

        canConnection.listenForPdo(mappings, listener, connection);
    }

    @Override
    public Object write(DeviceConnection connection,
                        List<ChannelValueContainer> containers,
                        Object containerListHandle)
            throws UnsupportedOperationException, ConnectionException {

        CanopenConnection canConnection = (CanopenConnection) connection.getConnectionHandle();

        for (ChannelValueContainer container : containers) {
            try {
                SDOObject sdoObject = null;
                if (container.getChannelHandle() == null) {
                    sdoObject = new SDOObject(container.getChannelAddress());
                    container.setChannelHandle(sdoObject);
                } else {
                    sdoObject = (SDOObject) container.getChannelHandle();
                }
                canConnection.writeSDO(sdoObject, container.getValue(), DEFAULT_TIMEOUT);
                container.setFlag(Flag.VALID);
            }
            catch (ArgumentSyntaxException e) {
                logger.warn("write failed: channel address syntax invalid {}",
                            container.getChannelAddress());
                container.setFlag(Flag.DRIVER_ERROR_CHANNEL_ADDRESS_SYNTAX_INVALID);
            }
            catch (CanException e) {
                logger.warn("write failed: {}", e.getMessage());
                container.setFlag(Flag.UNKNOWN_ERROR);
            }
        }

        return null;
    }

}
