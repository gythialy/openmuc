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

import org.openmuc.framework.data.ByteArrayValue;
import org.openmuc.framework.data.Flag;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.data.Value;
import org.openmuc.framework.driver.spi.ChannelRecordContainer;
import org.openmuc.framework.driver.spi.ConnectionException;
import org.openmuc.framework.driver.spi.DeviceConnection;
import org.openmuc.framework.driver.spi.RecordsReceivedListener;
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

import java.util.ArrayList;
import java.util.List;

/**
 * @author Frederic Robra
 */
public class CanopenConnection {

    private static Logger logger = LoggerFactory.getLogger(CanopenConnection.class);

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
        }
        catch (CanLinkException e) {
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
        Record record = createRecord(data,
                                     System.currentTimeMillis(),
                                     sdoObject.getNumericDataType());
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

    public void listenForPdo(final PDOMapping[] pdoMappings, final RecordsReceivedListener listener,
                             final DeviceConnection connection) {
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
                    Record record = createRecord(object.getData(),
                                                 timestamp,
                                                 object.getNumericDataType());
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
                listener.connectionInterrupted(connection);
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
}
