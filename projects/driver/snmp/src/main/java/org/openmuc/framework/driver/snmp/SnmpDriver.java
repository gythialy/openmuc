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
package org.openmuc.framework.driver.snmp;

import org.openmuc.framework.config.*;
import org.openmuc.framework.data.ByteArrayValue;
import org.openmuc.framework.data.Flag;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.driver.snmp.implementation.SnmpDevice;
import org.openmuc.framework.driver.snmp.implementation.SnmpDevice.SNMPVersion;
import org.openmuc.framework.driver.snmp.implementation.SnmpDeviceV1V2c;
import org.openmuc.framework.driver.snmp.implementation.SnmpDeviceV3;
import org.openmuc.framework.driver.snmp.implementation.SnmpTimeoutException;
import org.openmuc.framework.driver.spi.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author Mehran Shakeri
 */
public final class SnmpDriver implements DriverService {

    private final static Logger logger = LoggerFactory.getLogger(SnmpDriver.class);

    private final static DriverInfo info = new DriverInfo("snmp",
                                                          "snmp v1/v2c/v3 are supported.",
                                                          "?",
                                                          "?",
                                                          "?",
                                                          "?",
                                                          "?");

    // AUTHENTICATIONPASSPHRASE is the same COMMUNITY word in SNMP V2c
    public enum SnmpDriverSettingVariableNames {
        SNMPVersion, USERNAME, SECURITYNAME, AUTHENTICATIONPASSPHRASE, PRIVACYPASSPHRASE
    }

    ;

    // AUTHENTICATIONPASSPHRASE is the same COMMUNITY word in SNMP V2c
    public enum SnmpDriverScanSettingVariableNames {
        SNMPVersion, USERNAME, SECURITYNAME, AUTHENTICATIONPASSPHRASE, PRIVACYPASSPHRASE, STARTIP, ENDIP
    }

    ;

    private final static int timeout = 10000;

    // exception messages
    private final static String nullDeviceAddressException = "No device address found in config. Please specify one [eg. \"1.1.1.1/161\"].";
    private final static String nullSettingsException = "No device settings found in config. Please specify settings.";
    private final static String incorrectSettingsFormatException =
            "Format of setting string is invalid! \n Please use this format: "
            + "USERNAME=username:SECURITYNAME=securityname:AUTHENTICATIONPASSPHRASE=password:PRIVACYPASSPHRASE=privacy";
    private final static String incorrectSNMPVersionException = "Incorrect snmp version value. "
                                                                + "Please choose proper version. Possible values are defined in SNMPVersion enum";
    private final static String nullSNMPVersionException = "Snmp version is not defined. "
                                                           + "Please choose proper version. Possible values are defined in SNMPVersion enum";

    @Override
    public DriverInfo getInfo() {
        return info;
    }

    /**
     * Currently only supports SNMP V2c
     * <p/>
     * Default port number 161 is used
     *
     * @param settings at least must contain<br>
     *                 <p/>
     *                 <br>
     *                 SnmpDriverSettingVariableNames.AUTHENTICATIONPASSPHRASE: (community word) in case of more than on
     *                 value, they should be separated by ",". No community word is allowed to contain "," <br>
     *                 SnmpDriverScanSettingVariableNames.STARTIP: Start of IP range <br>
     *                 SnmpDriverScanSettingVariableNames.ENDIP: End of IP range <br>
     *                 eg. "AUTHENTICATIONPASSPHRASE=community,public:STARTIP=1.1.1.1:ENDIP=1.10.1.1"
     */
    @Override
    public void scanForDevices(String settings, DriverDeviceScanListener listener)
            throws ArgumentSyntaxException,
            ScanException, ScanInterruptedException {

        Map<String, String> settingMapper = settingParser(settings);

        // Current implementation is only for SNMP version 2c
        SnmpDeviceV1V2c snmpScanner;
        try {
            snmpScanner = new SnmpDeviceV1V2c(SNMPVersion.V2c);
        }
        catch (ConnectionException e) {
            throw new ScanException(e.getMessage());
        }

        SnmpDriverDiscoveryListener discoveryListener = new SnmpDriverDiscoveryListener(listener);
        snmpScanner.addEventListener(discoveryListener);
        String[] communityWords = settingMapper.get(
                SnmpDriverScanSettingVariableNames.AUTHENTICATIONPASSPHRASE.toString()).split(",");
        snmpScanner.scanSnmpV2cEnabledDevices(settingMapper.get(SnmpDriverScanSettingVariableNames.STARTIP
                                                                        .toString()),
                                              settingMapper.get(SnmpDriverScanSettingVariableNames.ENDIP
                                                                        .toString()),
                                              communityWords);

    }

    @Override
    public void interruptDeviceScan() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * @param settings SNMPVersion=V2c:COMMUNITY=value:SECURITYNAME=value:AUTHENTICATIONPASSPHRASE=value:PRIVACYPASSPHRASE=
     *                 value
     * @throws ConnectionException
     * @throws ArgumentSyntaxException
     */
    @Override
    public Object connect(String interfaceAddress, String deviceAddress, String settings)
            throws ConnectionException,
            ArgumentSyntaxException {

        SnmpDevice device = null;
        SNMPVersion snmpVersion = null;

        // check arguments
        if (deviceAddress == null || deviceAddress.equals("")) {
            throw new ArgumentSyntaxException(nullDeviceAddressException);
        } else if (settings == null || settings.equals("")) {
            throw new ArgumentSyntaxException(nullSettingsException);
        } else {

            Map<String, String> mappedSettings = settingParser(settings);

            try {
                snmpVersion = SNMPVersion.valueOf(mappedSettings.get(SnmpDriverSettingVariableNames.SNMPVersion
                                                                             .toString()));
            }
            catch (IllegalArgumentException e) {
                throw new ArgumentSyntaxException(incorrectSNMPVersionException);
            }
            catch (NullPointerException e) {
                throw new ArgumentSyntaxException(nullSNMPVersionException);
            }

            // create SnmpDevice object based on SNMP version
            switch (snmpVersion) {
            case V1:
            case V2c:
                device = new SnmpDeviceV1V2c(snmpVersion, deviceAddress,
                                             mappedSettings.get(SnmpDriverSettingVariableNames.AUTHENTICATIONPASSPHRASE
                                                                        .toString()));
                break;
            case V3:
                device = new SnmpDeviceV3(deviceAddress,
                                          mappedSettings.get(SnmpDriverSettingVariableNames.USERNAME
                                                                     .toString()),
                                          mappedSettings.get(SnmpDriverSettingVariableNames.SECURITYNAME
                                                                     .toString()),
                                          mappedSettings.get(SnmpDriverSettingVariableNames.AUTHENTICATIONPASSPHRASE
                                                                     .toString()),
                                          mappedSettings.get(SnmpDriverSettingVariableNames.PRIVACYPASSPHRASE
                                                                     .toString()));
                break;

            default:
                throw new ArgumentSyntaxException(incorrectSNMPVersionException);
            }

        }

        return device;
    }

    @Override
    public void disconnect(DeviceConnection connection) {
        connection = null;
    }

    /**
     * At least device address and channel address must be specified in the container.<br>
     * <br>
     * containers.deviceAddress = device address (eg. 1.1.1.1/161) <br>
     * containers.channelAddress = OID (eg. 1.3.6.1.2.1.1.0)
     */
    @Override
    public Object read(DeviceConnection connection, List<ChannelRecordContainer> containers,
                       Object containerListHandle, String samplingGroup)
            throws ConnectionException {

        SnmpDevice device = (SnmpDevice) connection.getConnectionHandle();
        return readChannelGroup(device, containers, timeout);
    }

    @Override
    public Object write(DeviceConnection connection,
                        List<ChannelValueContainer> containers,
                        Object containerListHandle)
            throws UnsupportedOperationException, ConnectionException {

        // TODO snmp set request will be implemented here
        throw new UnsupportedOperationException();
    }

    /**
     * Read all the channels of the device at once.
     *
     * @param device
     * @param containers
     * @param timeout
     * @return Object
     * @throws ConnectionException
     */
    private Object readChannelGroup(SnmpDevice device,
                                    List<ChannelRecordContainer> containers,
                                    int timeout)
            throws ConnectionException {

        new Date().getTime();

        List<String> oids = new ArrayList<String>();

        for (ChannelRecordContainer container : containers) {
            if (device.getDeviceAddress()
                      .equalsIgnoreCase(container.getChannel().getDeviceAddress())) {
                oids.add(container.getChannelAddress());
            }
        }

        Map<String, String> values = new HashMap<String, String>();

        try {
            values = device.getRequestsList(oids);
            long receiveTime = System.currentTimeMillis();

            for (ChannelRecordContainer container : containers) {
                // make sure the value exists for corresponding channel
                if (values.get(container.getChannelAddress()) != null) {
                    logger.debug("{}: value = '{}'", container.getChannelAddress(),
                                 values.get(container.getChannelAddress()));
                    container.setRecord(new Record(new ByteArrayValue(values.get(container.getChannelAddress())
                                                                            .getBytes()),
                                                   receiveTime));
                }
            }
        }
        catch (SnmpTimeoutException e) {
            for (ChannelRecordContainer container : containers) {
                container.setRecord(new Record(Flag.TIMEOUT));
            }
        }

        return null;
    }

    @Override
    public void startListening(DeviceConnection connection, List<ChannelRecordContainer> containers,
                               RecordsReceivedListener listener)
            throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ChannelScanInfo> scanForChannels(DeviceConnection connection, String settings)
            throws UnsupportedOperationException, ConnectionException {
        // not supported
        throw new UnsupportedOperationException();
    }

    /**
     * Read settings string and put them in a Key,Value HashMap Each setting consists of a pair of key/value and is
     * separated by ":" from other settings Inside the setting string, key and value are separated by "=" e.g.
     * "key1=value1:key2=value3" Be careful! "=" and ":" are not allowed in keys and values
     * <p/>
     * if your key contains more than one value, you can separate values by ",". in this case "," is not allowed in
     * values.
     *
     * @param settings
     * @return Map<String,String>
     * @throws ArgumentSyntaxException
     */
    private Map<String, String> settingParser(String settings) throws ArgumentSyntaxException {

        Map<String, String> settingsMaper = new HashMap<String, String>();

        try {
            String[] settingsArray = settings.split(":");
            for (String setting : settingsArray) {
                String[] keyValue = setting.split("=", 2);
                settingsMaper.put(keyValue[0], keyValue[1]);
            }
        }
        catch (ArrayIndexOutOfBoundsException e) {
            throw new ArgumentSyntaxException(incorrectSettingsFormatException);
        }
        return settingsMaper;
    }
}
