/*
 * Copyright 2011-2022 Fraunhofer ISE
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
package org.openmuc.framework.driver.dlms;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.openmuc.framework.config.ArgumentSyntaxException;
import org.openmuc.framework.config.ChannelScanInfo;
import org.openmuc.framework.data.ValueType;
import org.openmuc.framework.driver.dlms.settings.DeviceAddress;
import org.openmuc.framework.driver.dlms.settings.DeviceSettings;
import org.openmuc.framework.driver.spi.ChannelRecordContainer;
import org.openmuc.framework.driver.spi.ChannelValueContainer;
import org.openmuc.framework.driver.spi.Connection;
import org.openmuc.framework.driver.spi.ConnectionException;
import org.openmuc.framework.driver.spi.RecordsReceivedListener;
import org.openmuc.jdlms.AccessResultCode;
import org.openmuc.jdlms.AttributeAddress;
import org.openmuc.jdlms.DlmsConnection;
import org.openmuc.jdlms.GetResult;
import org.openmuc.jdlms.ObisCode;
import org.openmuc.jdlms.datatypes.DataObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class DlmsCosemConnection implements Connection {

    private static final Logger logger = LoggerFactory.getLogger(DlmsCosemConnection.class);

    private final DlmsConnection dlmsConnection;
    private final DeviceAddress deviceAddress;
    private final DeviceSettings deviceSettings;

    private final ReadHandle readHandle;
    private final WriteHandle writeHandle;

    public DlmsCosemConnection(String deviceAddress, String settings)
            throws ArgumentSyntaxException, ConnectionException {
        this.deviceAddress = new DeviceAddress(deviceAddress);
        this.deviceSettings = new DeviceSettings(settings);

        this.dlmsConnection = Connector.buildDlmsConection(this.deviceAddress, deviceSettings);

        this.readHandle = new ReadHandle(this.dlmsConnection);
        this.writeHandle = new WriteHandle(dlmsConnection);
    }

    @Override
    public void disconnect() {
        try {
            this.dlmsConnection.close();
        } catch (IOException e) {
            logger.warn("Failed to close DLMS connection.", e);
        }

    }

    @Override
    public Object read(List<ChannelRecordContainer> containers, Object containerListHandle, String samplingGroup)
            throws ConnectionException {
        this.readHandle.read(containers);

        return null;
    }

    @Override
    public Object write(List<ChannelValueContainer> containers, Object containerListHandle) throws ConnectionException {
        this.writeHandle.write(containers);

        return null;
    }

    @Override
    public List<ChannelScanInfo> scanForChannels(String settings) throws ConnectionException {
        if (deviceSettings.useSn()) {
            throw new UnsupportedOperationException("Scan devices for SN is not supported, yet.");
        }

        AttributeAddress scanChannel = new AttributeAddress(15, "0.0.40.0.0.255", 2);
        GetResult scanResult = executeScan(scanChannel);
        if (scanResult.getResultCode() != AccessResultCode.SUCCESS) {
            logger.error("Cannot scan device for channels. Resultcode: " + scanResult.getResultCode());
            throw new ConnectionException("Cannot scan device for channels.");
        }

        List<DataObject> objectArray = scanResult.getResultData().getValue();
        List<ChannelScanInfo> result = new ArrayList<>(objectArray.size());
        for (DataObject objectDef : objectArray) {
            List<DataObject> defItems = objectDef.getValue();

            int classId = defItems.get(0).getValue();
            classId &= 0xFF;

            byte[] instanceId = defItems.get(2).getValue();
            List<DataObject> accessRight = defItems.get(3).getValue();
            List<DataObject> attributes = accessRight.get(0).getValue();

            for (DataObject attributeAccess : attributes) {
                ChannelScanInfo scanInfo = createScanInfoFor(classId, instanceId, attributeAccess);
                result.add(scanInfo);
            }
        }
        return result;
    }

    private GetResult executeScan(AttributeAddress scanChannel) throws ConnectionException {
        try {
            return this.dlmsConnection.get(scanChannel);
        } catch (IOException e) {
            throw new ConnectionException("Problem to do action.", e);
        }
    }

    @Override
    public void startListening(List<ChannelRecordContainer> containers, RecordsReceivedListener listener)
            throws ConnectionException {
        throw new UnsupportedOperationException();
    }

    private static ChannelScanInfo createScanInfoFor(int classId, byte[] logicalName, DataObject attributeAccess) {
        List<DataObject> value = attributeAccess.getValue();
        int attributeId = extractNumVal(value.get(0));

        AttributeAccessMode accessMode = AttributeAccessMode.accessModeFor(value.get(1));

        ObisCode instanceId = new ObisCode(logicalName);

        String channelAddress = MessageFormat.format("a={0}/{1}/{2}", classId, instanceId, attributeId);

        int valueTypeLength = 0;

        // TODO: more/better description
        String description = channelAddress;

        return new ChannelScanInfo(channelAddress, description, ValueType.DOUBLE, valueTypeLength,
                accessMode.isReadable(), accessMode.isWriteable());

    }

    private static int extractNumVal(DataObject dataObject) {
        Number attributeId = dataObject.getValue();
        return attributeId.intValue() & 0xFF;
    }

    private enum AttributeAccessMode {
        NO_ACCESS(0, false, false),
        READ_ONLY(1, true, false),
        WRITE_ONLY(2, false, true),
        READ_AND_WRITE(3, true, true),
        AUTHENTICATED_READ_ONLY(4, true, false),
        AUTHENTICATED_WRITE_ONLY(5, false, true),
        AUTHENTICATED_READ_AND_WRITE(6, true, true),

        UNKNOWN_ACCESS_MODE(-1, false, false);

        private int code;
        private boolean readable;
        private boolean writeable;

        private AttributeAccessMode(int code, boolean readable, boolean writeable) {
            this.code = code;
            this.readable = readable;
            this.writeable = writeable;
        }

        public boolean isReadable() {
            return readable;
        }

        public boolean isWriteable() {
            return writeable;
        }

        public static AttributeAccessMode accessModeFor(DataObject dataObject) {
            Number code = dataObject.getValue();
            return accessModeFor(code.intValue() & 0xFF);
        }

        public static AttributeAccessMode accessModeFor(int code) {
            for (AttributeAccessMode accessMode : values()) {
                if (accessMode.code == code) {
                    return accessMode;
                }
            }

            return UNKNOWN_ACCESS_MODE;
        }

    }

}
