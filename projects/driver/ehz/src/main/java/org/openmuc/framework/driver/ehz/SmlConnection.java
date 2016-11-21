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

package org.openmuc.framework.driver.ehz;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.openmuc.framework.config.ChannelScanInfo;
import org.openmuc.framework.data.ValueType;
import org.openmuc.framework.driver.spi.ChannelRecordContainer;
import org.openmuc.framework.driver.spi.ConnectionException;
import org.openmuc.jsml.structures.ASNObject;
import org.openmuc.jsml.structures.Integer16;
import org.openmuc.jsml.structures.Integer32;
import org.openmuc.jsml.structures.Integer64;
import org.openmuc.jsml.structures.Integer8;
import org.openmuc.jsml.structures.SML_File;
import org.openmuc.jsml.structures.SML_GetListRes;
import org.openmuc.jsml.structures.SML_ListEntry;
import org.openmuc.jsml.structures.SML_Message;
import org.openmuc.jsml.structures.SML_MessageBody;
import org.openmuc.jsml.structures.Unsigned16;
import org.openmuc.jsml.structures.Unsigned32;
import org.openmuc.jsml.structures.Unsigned64;
import org.openmuc.jsml.structures.Unsigned8;
import org.openmuc.jsml.tl.SML_SerialReceiver;

import gnu.io.PortInUseException;
import gnu.io.UnsupportedCommOperationException;

/**
 * @author Frederic Robra
 * 
 */
public class SmlConnection extends GeneralConnection {

    private final SML_SerialReceiver receiver;
    private String serverID;

    public SmlConnection(String deviceAddress) throws ConnectionException {
        name = "SML - " + deviceAddress + " - ";
        receiver = new SML_SerialReceiver();
        try {
            receiver.setupComPort(deviceAddress);
        } catch (IOException e) {
            throw new ConnectionException();
        } catch (PortInUseException e) {
            throw new ConnectionException("Port in use");
        } catch (UnsupportedCommOperationException e) {
            throw new ConnectionException("Unsupported comm operation");
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openmuc.framework.driver.ehz.Connection#close()
     */
    @Override
    public void close() {
        try {
            receiver.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openmuc.framework.driver.ehz.Connection#read(java.util.List, int)
     */
    @Override
    public void read(List<ChannelRecordContainer> containers, int timeout) throws ConnectionException {
        logger.trace(name + "reading channels");
        try {
            long timestamp = System.currentTimeMillis();
            SML_ListEntry[] list = getSML_ListEntries();

            Map<String, Double> values = new LinkedHashMap<>();
            for (SML_ListEntry entry : list) {
                String address = getAddress(entry.getObjName().getOctetString());
                double value = getValue(entry);
                values.put(address, value);
                logger.trace(name + address + " = " + value);
            }

            handleChannelRecordContainer(containers, values, timestamp);
        } catch (IOException e) {
            e.printStackTrace();
            logger.error(name + "read failed");
            close();
            throw new ConnectionException(e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openmuc.framework.driver.ehz.Connection#listChannels(int)
     */
    @Override
    public List<ChannelScanInfo> listChannels(int timeout) {
        List<ChannelScanInfo> channelInfos = new LinkedList<>();

        logger.debug(name + "scanning channels");
        try {
            SML_ListEntry[] list = getSML_ListEntries();
            for (SML_ListEntry entry : list) {
                String channelAddress = getAddress(entry.getObjName().getOctetString());
                String description = "Current value: " + getValue(entry); // TODO entry.getUnit();
                ValueType valueType = ValueType.DOUBLE;
                Integer valueTypeLength = null;
                Boolean readable = true;
                Boolean writable = false;
                ChannelScanInfo channelInfo = new ChannelScanInfo(channelAddress, description, valueType,
                        valueTypeLength, readable, writable);
                channelInfos.add(channelInfo);
            }
        } catch (IOException e) {
            e.printStackTrace();
            logger.error(name + "read failed");
        }
        return channelInfos;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openmuc.framework.driver.ehz.Connection#isWorking()
     */
    @Override
    public boolean isWorking() {
        try {
            getSML_ListEntries();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private SML_ListEntry[] getSML_ListEntries() throws IOException {
        SML_File smlFile = receiver.getSMLFile();

        List<SML_Message> messages = smlFile.getMessages();

        SML_ListEntry[] list = null;

        for (SML_Message message : messages) {
            int tag = message.getMessageBody().getTag().getVal();

            if (tag == SML_MessageBody.GetListResponse) {
                SML_GetListRes resp = (SML_GetListRes) message.getMessageBody().getChoice();

                if (serverID == null) {
                    serverID = "";
                    for (Byte b : resp.getServerId().getOctetString()) {
                        serverID += Integer.toString((b & 0xff) + 0x100, 16).substring(1);
                    }
                    serverID = serverID.toUpperCase();
                }

                list = resp.getValList().getValListEntry();
                break;
            }
        }
        return list;
    }

    private String getAddress(byte[] data) {
        StringBuilder address = new StringBuilder(data.length * 2);
        for (byte b : data) {
            address.append(String.format("%x", b));
        }
        return address.toString();
    }

    // TODO return OpenMUC value
    private double getValue(SML_ListEntry entry) {
        double value = 0;

        ASNObject obj = entry.getValue().getChoice();
        if (obj.getClass().equals(Integer64.class)) {
            Integer64 val = (Integer64) obj;
            value = val.getVal();
        }
        else if (obj.getClass().equals(Integer32.class)) {
            Integer32 val = (Integer32) obj;
            value = val.getVal();
        }
        else if (obj.getClass().equals(Integer16.class)) {
            Integer16 val = (Integer16) obj;
            value = val.getVal();
        }
        else if (obj.getClass().equals(Integer8.class)) {
            Integer8 val = (Integer8) obj;
            value = val.getVal();
        }
        else if (obj.getClass().equals(Unsigned64.class)) {
            Unsigned64 val = (Unsigned64) obj;
            value = val.getVal();
        }
        else if (obj.getClass().equals(Unsigned32.class)) {
            Unsigned32 val = (Unsigned32) obj;
            value = val.getVal();
        }
        else if (obj.getClass().equals(Unsigned16.class)) {
            Unsigned16 val = (Unsigned16) obj;
            value = val.getVal();
        }
        else if (obj.getClass().equals(Unsigned8.class)) {
            Unsigned8 val = (Unsigned8) obj;
            value = val.getVal();
        }
        else {
            return Double.NaN;
        }

        byte scaler = entry.getScaler().getVal();
        return value * Math.pow(10, scaler);
    }

}
