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

import org.openmuc.framework.config.ArgumentSyntaxException;
import org.openmuc.framework.config.ChannelScanInfo;
import org.openmuc.framework.config.ScanException;
import org.openmuc.framework.data.ByteArrayValue;
import org.openmuc.framework.data.Flag;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.data.Value;
import org.openmuc.framework.driver.spi.*;
import org.openmuc.jcanopen.datatypes.NumericDataType;
import org.openmuc.jcanopen.exc.CanException;
import org.openmuc.jcanopen.exc.CanLinkException;
import org.openmuc.jcanopen.link.CanLink;
import org.openmuc.jcanopen.mngt.NMT;
import org.openmuc.jcanopen.mngt.SYNC;
import org.openmuc.jcanopen.pdo.PDOListener;
import org.openmuc.jcanopen.pdo.PDOMapping;
import org.openmuc.jcanopen.pdo.PDOObject;
import org.openmuc.jcanopen.pdo.PDOReceiver;
import org.openmuc.jcanopen.sdo.SDOClient;
import org.openmuc.jcanopen.socketcan.CanLinkSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @author Frederic Robra
 */
public class CanopenConnection implements Connection {

    private static Logger logger = LoggerFactory.getLogger(CanopenConnection.class);
    private static final long DEFAULT_TIMEOUT = 5000;

    CanLink link;

    private PDOListener pdoListener;
    private PDOReceiver receiver;

    private SYNC sync;
    private boolean startSync = false;
    private long syncTime;
    private NMT nmt;

    public CanopenConnection(String ifname) throws ConnectionException {
        try {
            link = new CanLinkSocket(ifname);
        } catch (CanLinkException e) {
            throw new ConnectionException(e);
        }

    }

    public void startNMT() throws CanException {
        nmt = new NMT(link);
        nmt.write(NMT.CommandSpecifier.RESET_NODE, 0);
        nmt.write(NMT.CommandSpecifier.START_REMOTE_NODE, 0);
        if (startSync) {
            startSync(syncTime);
        }
    }

    public void startSync(long time) {
        startSync = true;
        syncTime = time;
        if (nmt != null) {
            sync = new SYNC(link);
            sync.startPeriodicallySync(time);
        }
    }

    public Record readSDO(SDOObject sdoObject, long timeout) throws CanException {
        SDOClient client = new SDOClient(link, sdoObject.getNodeId());
        byte[] data = client.upload(sdoObject.getIndex(), sdoObject.getSubIndex(), timeout);
        Record record = createRecord(data, System.currentTimeMillis(), sdoObject.getNumericDataType());
        return record;
    }

    public void writeSDO(SDOObject sdoObject, Value value, long timeout) throws CanException {
        SDOClient client = new SDOClient(link, sdoObject.getNodeId());
        byte[] data = null;
        if (sdoObject.getNumericDataType() != null) {
            NumericDataType dataType = sdoObject.getNumericDataType();
            dataType.setValue(Transforms.value2Number(value));
            data = dataType.getData();
        } else {
            data = value.asByteArray();
        }

        client.download(sdoObject.getIndex(), sdoObject.getSubIndex(), data, timeout);
    }

    public void listenForPdo(final PDOMapping[] pdoMappings, final RecordsReceivedListener listener) {
        if (receiver == null) {
            receiver = new PDOReceiver(link);
        } else if (pdoListener != null) {
            receiver.removeListener(pdoListener);
        }

        pdoListener = new PDOListener() {

            @Override
            public void received(int cobId, PDOObject[] objects) {
                Long timestamp = System.currentTimeMillis();
                List<ChannelRecordContainer> recordContainers = new ArrayList<ChannelRecordContainer>();
                for (PDOObject pdoObject : objects) {
                    PDOObjectImpl object = (PDOObjectImpl) pdoObject;
                    Record record = createRecord(object.getData(), timestamp, object.getNumericDataType());
                    ChannelRecordContainer container = object.getContainer();
                    container.setRecord(record);
                    recordContainers.add(container);
                }
                listener.newRecords(recordContainers);
            }

            @Override
            public PDOMapping[] getPDOMappings() {
                return pdoMappings;
            }

            @Override
            public void closed() {
                listener.connectionInterrupted(CanopenDriver.driverInfo.getId(), CanopenConnection.this);
            }
        };

        receiver.addListener(pdoListener);
    }

    public void close() {
        if (sync != null) {
            sync.stopPeriodicallySync();
        }
        if (nmt != null) {
            nmt.detach();
        }
        if (receiver != null) {
            receiver.detach();
        }
        link.close();
    }

    private static Record createRecord(byte[] data, long timestamp, NumericDataType dataType) {
        Value value = null;
        Flag flag = Flag.UNKNOWN_ERROR;
        if (data != null) {
            if (dataType != null) {
                dataType.setData(data);
                logger.trace("received PDO value: {}", dataType.getValue());
                value = Transforms.number2Value(dataType.getValue());
            } else {
                value = new ByteArrayValue(data);
            }
            flag = Flag.VALID;
        }

        return new Record(value, timestamp, flag);
    }

    @Override
    public List<ChannelScanInfo> scanForChannels(String settings) throws UnsupportedOperationException, ArgumentSyntaxException,
            ScanException, ConnectionException {

        List<ChannelScanInfo> infos = new LinkedList<ChannelScanInfo>();
        ExecutorService executor = Executors.newCachedThreadPool();
        Collection<ChannelScanner> tasks = new LinkedList<ChannelScanner>();
        int steps = 10;
        for (int i = 1; i < 256; i += steps) {
            tasks.add(new ChannelScanner(i, i + steps, this));
        }
        try {
            List<Future<List<ChannelScanInfo>>> results = executor.invokeAll(tasks);

            for (Future<List<ChannelScanInfo>> result : results) {
                infos.addAll(result.get());
            }

        } catch (Exception e) {
            logger.warn("failed to scan channels: {}", e.getMessage());
        }

        return infos;
    }

    @Override
    public Object read(List<ChannelRecordContainer> containers, Object containerListHandle, String samplingGroup) throws
            UnsupportedOperationException, ConnectionException {

        for (ChannelRecordContainer container : containers) {
            try {
                SDOObject sdoObject = null;
                if (container.getChannelHandle() == null) {
                    sdoObject = new SDOObject(container.getChannelAddress());
                    container.setChannelHandle(sdoObject);
                } else {
                    sdoObject = (SDOObject) container.getChannelHandle();
                }
                Record record = readSDO(sdoObject, DEFAULT_TIMEOUT);
                container.setRecord(record);
            } catch (ArgumentSyntaxException e) {
                logger.warn("read failed: channel address syntax invalid: {}", container.getChannelAddress());
                Record record = new Record(null, System.currentTimeMillis(), Flag.DRIVER_ERROR_CHANNEL_ADDRESS_SYNTAX_INVALID);
                container.setRecord(record);
            } catch (CanException e) {
                logger.warn("read failed: {}", e.getMessage());
                Record record = new Record(null, System.currentTimeMillis(), Flag.UNKNOWN_ERROR);
                container.setRecord(record);
            }
        }

        return null;
    }

    @Override
    public void startListening(List<ChannelRecordContainer> containers, RecordsReceivedListener listener) throws
            UnsupportedOperationException, ConnectionException {

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
            } catch (ArgumentSyntaxException e) {
                logger.warn("channel address syntax invalid {}", container.getChannelAddress());
                Record record = new Record(null, System.currentTimeMillis(), Flag.DRIVER_ERROR_CHANNEL_ADDRESS_SYNTAX_INVALID);
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

        listenForPdo(mappings, listener);
    }

    @Override
    public Object write(List<ChannelValueContainer> containers, Object containerListHandle) throws UnsupportedOperationException,
            ConnectionException {

        for (ChannelValueContainer container : containers) {
            try {
                SDOObject sdoObject = null;
                if (container.getChannelHandle() == null) {
                    sdoObject = new SDOObject(container.getChannelAddress());
                    container.setChannelHandle(sdoObject);
                } else {
                    sdoObject = (SDOObject) container.getChannelHandle();
                }
                writeSDO(sdoObject, container.getValue(), DEFAULT_TIMEOUT);
                container.setFlag(Flag.VALID);
            } catch (ArgumentSyntaxException e) {
                logger.warn("write failed: channel address syntax invalid {}", container.getChannelAddress());
                container.setFlag(Flag.DRIVER_ERROR_CHANNEL_ADDRESS_SYNTAX_INVALID);
            } catch (CanException e) {
                logger.warn("write failed: {}", e.getMessage());
                container.setFlag(Flag.UNKNOWN_ERROR);
            }
        }

        return null;
    }

    @Override
    public void disconnect() {
        close();
    }
}
