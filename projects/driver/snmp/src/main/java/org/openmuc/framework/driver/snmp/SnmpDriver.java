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
package org.openmuc.framework.driver.snmp;

import java.util.HashMap;
import java.util.Map;

import org.openmuc.framework.config.ArgumentSyntaxException;
import org.openmuc.framework.config.DriverInfo;
import org.openmuc.framework.config.ScanException;
import org.openmuc.framework.config.ScanInterruptedException;
import org.openmuc.framework.driver.snmp.implementation.SnmpDevice;
import org.openmuc.framework.driver.snmp.implementation.SnmpDevice.SNMPVersion;
import org.openmuc.framework.driver.snmp.implementation.SnmpDeviceV1V2c;
import org.openmuc.framework.driver.snmp.implementation.SnmpDeviceV3;
import org.openmuc.framework.driver.spi.Connection;
import org.openmuc.framework.driver.spi.ConnectionException;
import org.openmuc.framework.driver.spi.DriverDeviceScanListener;
import org.openmuc.framework.driver.spi.DriverService;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public final class SnmpDriver implements DriverService {

    private static final Logger logger = LoggerFactory.getLogger(SnmpDriver.class);

    private static final DriverInfo info = new DriverInfo("snmp", "snmp v1/v2c/v3 are supported.", "?", "?", "?", "?");

    // AUTHENTICATIONPASSPHRASE is the same COMMUNITY word in SNMP V2c
    public enum SnmpDriverSettingVariableNames {
        SNMP_VERSION,
        USERNAME,
        SECURITYNAME,
        AUTHENTICATIONPASSPHRASE,
        PRIVACYPASSPHRASE
    }

    // AUTHENTICATIONPASSPHRASE is the same COMMUNITY word in SNMP V2c
    public enum SnmpDriverScanSettingVariableNames {
        SNMP_VERSION,
        USERNAME,
        SECURITYNAME,
        AUTHENTICATIONPASSPHRASE,
        PRIVACYPASSPHRASE,
        STARTIP,
        ENDIP
    }

    // exception messages
    private static final String NULL_DEVICE_ADDRESS_EXCEPTION = "No device address found in config. Please specify one [eg. \"1.1.1.1/161\"].";
    private static final String NULL_SETTINGS_EXCEPTION = "No device settings found in config. Please specify settings.";
    private static final String INCORRECT_SETTINGS_FORMAT_EXCEPTION = "Format of setting string is invalid! \n Please use this format: "
            + "USERNAME=username:SECURITYNAME=securityname:AUTHENTICATIONPASSPHRASE=password:PRIVACYPASSPHRASE=privacy";
    private static final String INCORRECT_SNMP_VERSION_EXCEPTION = "Incorrect snmp version value. "
            + "Please choose proper version. Possible values are defined in SNMPVersion enum";
    private static final String NULL_SNMP_VERSION_EXCEPTION = "Snmp version is not defined. "
            + "Please choose proper version. Possible values are defined in SNMPVersion enum";

    @Override
    public DriverInfo getInfo() {
        return info;
    }

    /**
     * Currently only supports SNMP V2c
     * 
     * Default port number 161 is used
     * 
     * @param settings
     *            at least must contain<br>
     * 
     *            <br>
     *            SnmpDriverSettingVariableNames.AUTHENTICATIONPASSPHRASE: (community word) in case of more than on
     *            value, they should be separated by ",". No community word is allowed to contain "," <br>
     *            SnmpDriverScanSettingVariableNames.STARTIP: Start of IP range <br>
     *            SnmpDriverScanSettingVariableNames.ENDIP: End of IP range <br>
     *            eg. "AUTHENTICATIONPASSPHRASE=community,public:STARTIP=1.1.1.1:ENDIP=1.10.1.1"
     * 
     */
    @Override
    public void scanForDevices(String settings, DriverDeviceScanListener listener)
            throws ArgumentSyntaxException, ScanException, ScanInterruptedException {

        Map<String, String> settingMapper = settingParser(settings);

        // Current implementation is only for SNMP version 2c
        SnmpDeviceV1V2c snmpScanner;
        try {
            snmpScanner = new SnmpDeviceV1V2c(SNMPVersion.V2c);
        } catch (ConnectionException e) {
            throw new ScanException(e.getMessage());
        }

        SnmpDriverDiscoveryListener discoveryListener = new SnmpDriverDiscoveryListener(listener);
        snmpScanner.addEventListener(discoveryListener);
        String[] communityWords = settingMapper
                .get(SnmpDriverScanSettingVariableNames.AUTHENTICATIONPASSPHRASE.toString())
                .split(",");
        snmpScanner.scanSnmpV2cEnabledDevices(settingMapper.get(SnmpDriverScanSettingVariableNames.STARTIP.toString()),
                settingMapper.get(SnmpDriverScanSettingVariableNames.ENDIP.toString()), communityWords);

    }

    @Override
    public void interruptDeviceScan() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * 
     * @param settings
     *            SNMPVersion=V2c:COMMUNITY=value:SECURITYNAME=value:AUTHENTICATIONPASSPHRASE=value:PRIVACYPASSPHRASE=
     *            value
     * 
     * @throws ConnectionException
     *             thrown if SNMP listen or initialization failed
     * @throws ArgumentSyntaxException
     *             thrown if Device address foramt is wrong
     */
    @Override
    public Connection connect(String deviceAddress, String settings)
            throws ConnectionException, ArgumentSyntaxException {

        SnmpDevice device = null;
        SNMPVersion snmpVersion = null;

        // check arguments
        if (deviceAddress == null || deviceAddress.equals("")) {
            throw new ArgumentSyntaxException(NULL_DEVICE_ADDRESS_EXCEPTION);
        }
        else if (settings == null || settings.equals("")) {
            throw new ArgumentSyntaxException(NULL_SETTINGS_EXCEPTION);
        }
        else {

            Map<String, String> mappedSettings = settingParser(settings);

            try {
                snmpVersion = SNMPVersion
                        .valueOf(mappedSettings.get(SnmpDriverSettingVariableNames.SNMP_VERSION.toString()));
            } catch (IllegalArgumentException e) {
                throw new ArgumentSyntaxException(INCORRECT_SNMP_VERSION_EXCEPTION);
            } catch (NullPointerException e) {
                throw new ArgumentSyntaxException(NULL_SNMP_VERSION_EXCEPTION);
            }

            // create SnmpDevice object based on SNMP version
            switch (snmpVersion) {
            case V1:
            case V2c:
                device = new SnmpDeviceV1V2c(snmpVersion, deviceAddress,
                        mappedSettings.get(SnmpDriverSettingVariableNames.AUTHENTICATIONPASSPHRASE.toString()));
                break;
            case V3:
                device = new SnmpDeviceV3(deviceAddress,
                        mappedSettings.get(SnmpDriverSettingVariableNames.USERNAME.toString()),
                        mappedSettings.get(SnmpDriverSettingVariableNames.SECURITYNAME.toString()),
                        mappedSettings.get(SnmpDriverSettingVariableNames.AUTHENTICATIONPASSPHRASE.toString()),
                        mappedSettings.get(SnmpDriverSettingVariableNames.PRIVACYPASSPHRASE.toString()));
                break;

            default:
                throw new ArgumentSyntaxException(INCORRECT_SNMP_VERSION_EXCEPTION);
            }

        }

        return device;
    }

    /**
     * Read settings string and put them in a Key,Value HashMap Each setting consists of a pair of key/value and is
     * separated by ":" from other settings Inside the setting string, key and value are separated by "=" e.g.
     * "key1=value1:key2=value3" Be careful! "=" and ":" are not allowed in keys and values
     * 
     * if your key contains more than one value, you can separate values by ",". in this case "," is not allowed in
     * values.
     * 
     * @param settings
     * @return Map<String,String>
     * @throws ArgumentSyntaxException
     */
    private Map<String, String> settingParser(String settings) throws ArgumentSyntaxException {

        Map<String, String> settingsMaper = new HashMap<>();

        try {
            String[] settingsArray = settings.split(":");
            for (String setting : settingsArray) {
                String[] keyValue = setting.split("=", 2);
                settingsMaper.put(keyValue[0], keyValue[1]);
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new ArgumentSyntaxException(INCORRECT_SETTINGS_FORMAT_EXCEPTION);
        }
        return settingsMaper;
    }

}
