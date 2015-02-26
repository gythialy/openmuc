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
package org.openmuc.framework.driver.mbus;

import org.openmuc.framework.config.ChannelScanInfo;
import org.openmuc.framework.data.*;
import org.openmuc.framework.driver.spi.*;
import org.openmuc.jmbus.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class MBusConnection implements Connection {
    private final static Logger logger = LoggerFactory.getLogger(MBusConnection.class);

    private final MBusSerialInterface serialInterface;
    private final int mBusAddress;

    public MBusConnection(MBusSerialInterface serialInterface, int mBusAddress) {
        this.serialInterface = serialInterface;
        this.mBusAddress = mBusAddress;
    }

    @Override
    public List<ChannelScanInfo> scanForChannels(String settings) throws UnsupportedOperationException, ConnectionException {

        synchronized (serialInterface) {

            List<ChannelScanInfo> chanScanInf = new ArrayList<ChannelScanInfo>();

            try {
                VariableDataStructure msg = serialInterface.getMBusSap().read(mBusAddress);
                msg.decodeDeep();

                List<DataRecord> vdb = msg.getDataRecords();

                for (DataRecord block : vdb) {

                    block.decode();

                    String vib = HexConverter.getShortHexStringFromByteArray(block.getVIB());
                    String dib = HexConverter.getShortHexStringFromByteArray(block.getDIB());

                    ValueType valueType;
                    Integer valueLength;

                    switch (block.getDataValueType()) {
                        case DATE:
                            valueType = ValueType.BYTE_ARRAY;
                            valueLength = 250;
                            break;
                        case STRING:
                            valueType = ValueType.BYTE_ARRAY;
                            valueLength = 250;
                            break;
                        case LONG:
                            valueType = ValueType.LONG;
                            valueLength = null;
                            break;
                        case DOUBLE:
                            valueType = ValueType.DOUBLE;
                            valueLength = null;
                            break;
                        default:
                            valueType = ValueType.BYTE_ARRAY;
                            valueLength = 250;
                            break;
                    }

                    chanScanInf.add(new ChannelScanInfo(dib + ":" + vib, block.getDescription().toString(), valueType, valueLength));
                }
            } catch (IOException e) {
                throw new ConnectionException(e);
            } catch (TimeoutException e) {
                return null;
            } catch (DecodingException e) {
                e.printStackTrace();
                logger.debug("Skipped invalid or unsupported M-Bus VariableDataBlock:" + e.getMessage());
            }

            return chanScanInf;

        }
    }

    @Override
    public void disconnect() {

        synchronized (serialInterface) {

            if (!serialInterface.isOpen()) {
                return;
            }

            serialInterface.decreaseConnectionCounter();
        }
    }

    @Override
    public Object read(List<ChannelRecordContainer> containers, Object containerListHandle, String samplingGroup) throws
            UnsupportedOperationException, ConnectionException {

        synchronized (serialInterface) {

            if (!serialInterface.isOpen()) {
                throw new ConnectionException();
            }

            VariableDataStructure response = null;
            try {
                response = serialInterface.getMBusSap().read(mBusAddress);
                response.decodeDeep();
            } catch (IOException e1) {
                serialInterface.close();
                throw new ConnectionException(e1);
            } catch (TimeoutException e1) {
                for (ChannelRecordContainer container : containers) {
                    container.setRecord(new Record(Flag.TIMEOUT));
                }
                return null;
            } catch (DecodingException e) {
                e.printStackTrace();
            }

            long timestamp = System.currentTimeMillis();

            List<DataRecord> vdbs = response.getDataRecords();
            String[] dibvibs = new String[vdbs.size()];

            int i = 0;
            for (DataRecord vdb : vdbs) {
                dibvibs[i++] = HexConverter.getShortHexStringFromByteArray(vdb.getDIB()) + ':' + HexConverter
                        .getShortHexStringFromByteArray(vdb.getVIB());
            }

            for (ChannelRecordContainer container : containers) {

                i = 0;
                for (DataRecord dataBlock : vdbs) {

                    if (dibvibs[i++].equalsIgnoreCase(container.getChannelAddress())) {

                        try {
                            dataBlock.decode();
                        } catch (DecodingException e) {
                            container.setRecord(new Record(Flag.DRIVER_ERROR_CHANNEL_NOT_ACCESSIBLE));
                            logger.debug("Unable to parse VariableDataBlock received via M-Bus", e);
                            break;
                        }

                        switch (dataBlock.getDataValueType()) {
                            case DATE:
                                container.setRecord(new Record(new StringValue(((Date) dataBlock.getDataValue()).toString()), timestamp));
                                break;
                            case STRING:
                                container.setRecord(new Record(new StringValue((String) dataBlock.getDataValue()), timestamp));
                                break;
                            case DOUBLE:
                                container.setRecord(new Record(new DoubleValue(dataBlock.getScaledDataValue()), timestamp));
                                break;
                            case LONG:
                                if (dataBlock.getMultiplierExponent() == 0) {
                                    container.setRecord(new Record(new LongValue((Long) dataBlock.getDataValue()), timestamp));
                                } else {
                                    container.setRecord(new Record(new DoubleValue(dataBlock.getScaledDataValue()), timestamp));
                                }
                                break;
                            case BCD:
                                if (dataBlock.getMultiplierExponent() == 0) {
                                    container.setRecord(new Record(new LongValue(((Bcd) dataBlock.getDataValue()).longValue()), timestamp));
                                } else {
                                    container.setRecord(new Record(new DoubleValue(
                                            ((Bcd) dataBlock.getDataValue()).longValue() * Math.pow(10, dataBlock.getMultiplierExponent())),
                                                                   timestamp));
                                }
                                break;
                            case NONE:
                                if (logger.isWarnEnabled()) {
                                    logger.warn("Received data record with <dib>:<vib> = " + dibvibs[i] + " has value type NONE.");
                                }
                                break;
                        }

                        break;

                    }

                }

                if (container.getRecord() == null) {
                    container.setRecord(new Record(Flag.DRIVER_ERROR_CHANNEL_WITH_THIS_ADDRESS_NOT_FOUND));
                }

            }

            return null;

        }
    }

    @Override
    public void startListening(List<ChannelRecordContainer> containers, RecordsReceivedListener listener) throws UnsupportedOperationException, ConnectionException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object write(List<ChannelValueContainer> containers, Object containerListHandle) throws UnsupportedOperationException, ConnectionException {
        throw new UnsupportedOperationException();
    }

}
