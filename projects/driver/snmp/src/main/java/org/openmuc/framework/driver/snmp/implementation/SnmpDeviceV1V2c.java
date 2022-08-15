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
package org.openmuc.framework.driver.snmp.implementation;

import java.io.IOException;
import java.util.List;

import org.openmuc.framework.config.ArgumentSyntaxException;
import org.openmuc.framework.driver.snmp.SnmpDriver.SnmpDriverSettingVariableNames;
import org.openmuc.framework.driver.spi.ConnectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.event.ResponseListener;
import org.snmp4j.mp.MPv3;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.security.SecurityModels;
import org.snmp4j.security.SecurityProtocols;
import org.snmp4j.security.USM;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;

public class SnmpDeviceV1V2c extends SnmpDevice {

    private static final Logger logger = LoggerFactory.getLogger(SnmpDeviceV1V2c.class);

    private int snmpVersion;

    /**
     * snmp constructor takes primary parameters in order to create snmp object. this implementation uses UDP protocol
     * 
     * @param version
     *            Can be V1 or V2c corresponding to snmp v1 or v2c
     * @param address
     *            Contains ip and port. accepted string "X.X.X.X/portNo" or "udp:X.X.X.X/portNo"
     * @param authenticationPassphrase
     *            the authentication pass phrase. If not <code>null</code>, <code>authenticationProtocol</code> must
     *            also be not <code>null</code>. RFC3414 &sect;11.2 requires pass phrases to have a minimum length of 8
     *            bytes. If the length of <code>authenticationPassphrase</code> is less than 8 bytes an
     *            <code>IllegalArgumentException</code> is thrown. [required by snmp4j library]
     * 
     * @throws ConnectionException
     *             thrown if SNMP listen or initialization failed
     * @throws ArgumentSyntaxException
     *             thrown if Given snmp version is not correct or supported
     */

    public SnmpDeviceV1V2c(SNMPVersion version, String address, String authenticationPassphrase)
            throws ConnectionException, ArgumentSyntaxException {
        super(address, authenticationPassphrase);
        setVersion(version);
        setTarget();

    }

    /**
     * scanner constructor
     * 
     * @param version
     *            SNMP version
     * @throws ArgumentSyntaxException
     *             thrown if Given snmp version is not correct or supported
     * @throws ConnectionException
     *             thrown if SNMP listen failed
     */
    public SnmpDeviceV1V2c(SNMPVersion version) throws ArgumentSyntaxException, ConnectionException {
        setVersion(version);
        try {
            snmp = new Snmp(new DefaultUdpTransportMapping());
        } catch (IOException e) {
            throw new ConnectionException("SNMP initialization failed! \n" + e.getMessage());
        }
        usm = new USM(SecurityProtocols.getInstance(), new OctetString(MPv3.createLocalEngineID()), 0);
        SecurityModels.getInstance().addSecurityModel(usm);
        try {
            snmp.listen();
        } catch (IOException e) {
            throw new ConnectionException("SNMP listen failed! \n" + e.getMessage());
        }

    }

    /**
     * set SNMP version
     * 
     * @param version
     *            SNMP version
     * @throws ArgumentSyntaxException
     *             thrown if Given snmp version is not correct or supported
     */
    private void setVersion(SNMPVersion version) throws ArgumentSyntaxException {
        switch (version) {
        case V1:
            snmpVersion = SnmpConstants.version1;
            break;
        case V2c:
            snmpVersion = SnmpConstants.version2c;
            break;

        default:
            throw new ArgumentSyntaxException(
                    "Given snmp version is not correct or supported! Expected values are [V1,V2c].");
        }
    }

    @Override
    void setTarget() {
        target = new CommunityTarget();
        ((CommunityTarget) target).setCommunity(new OctetString(authenticationPassphrase));
        target.setAddress(targetAddress);
        target.setRetries(retries);
        target.setTimeout(timeout);
        target.setVersion(snmpVersion);
    }

    public String getInterfaceAddress() {
        return null;
    }

    @Override
    public String getDeviceAddress() {
        return targetAddress.toString();
    }

    public String getSettings() {
        return SnmpDriverSettingVariableNames.SNMP_VERSION.toString() + "="
                + getSnmpVersionFromSnmpConstantsValue(snmpVersion) + ":COMMUNITY=" + authenticationPassphrase;
    }

    public Object getConnectionHandle() {
        return this;
    }

    /**
     * Search for SNMP V2c enabled devices in network by sending proper SNMP GET request to given range of IP addresses.
     * 
     * For network and process efficiency, requests are sent to broadcast addresses (IP addresses ending with .255).
     * 
     * startIPRange can be greater than endIPRange. In this case, it will reach the last available address and start
     * from the first IP address again
     * 
     * @param startIPRange
     *            start of IP range
     * @param endIPRange
     *            en of IP range
     * @param communityWords
     *            community words
     * @throws ArgumentSyntaxException
     *             thrown if Given start ip address is not a valid IPV4 address
     */
    public void scanSnmpV2cEnabledDevices(String startIPRange, String endIPRange, String[] communityWords)
            throws ArgumentSyntaxException {

        // create PDU
        PDU pdu = new PDU();

        for (String oid : ScanOIDs.values()) {
            pdu.add(new VariableBinding(new OID(oid)));
        }
        pdu.setType(PDU.GET);

        // make sure the start/end IP is broadcast (eg. X.Y.Z.255)
        try {
            String[] ip = startIPRange.split("\\.");
            startIPRange = ip[0] + "." + ip[1] + "." + ip[2] + ".255";
            ip = endIPRange.split("\\.");
            endIPRange = ip[0] + "." + ip[1] + "." + ip[2] + ".255";
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new ArgumentSyntaxException("Given start ip address is not a valid IPV4 address.");
        }
        String nextIp = startIPRange;
        // in order to check also the EndIPRange
        endIPRange = getNextBroadcastIPV4Address(endIPRange);
        try {

            // loop through all IP addresses
            while (endIPRange.compareTo(nextIp) != 0) {
                // TODO scan progress can be implemented here

                // define broadcast address
                try {
                    targetAddress = GenericAddress.parse("udp:" + nextIp + "/161");
                } catch (IllegalArgumentException e) {
                    throw new ArgumentSyntaxException("Device address format is wrong! (eg. 1.1.1.255)");
                }

                // loop through all community words
                for (String community : communityWords) {

                    // set target V2c
                    authenticationPassphrase = community;
                    setTarget();

                    class ScanResponseListener implements ResponseListener {

                        @Override
                        @SuppressWarnings("unchecked")
                        public void onResponse(ResponseEvent event) {
                            /**
                             * Since we are sending broadcast request we have to keep async request alive. Otherwise
                             * async request must be cancel by blew code in order to prevent memory leak
                             * 
                             * ((Snmp)event.getSource()).cancel(event.getRequest (), this);
                             * 
                             */

                            if (event.getResponse() != null) {
                                @SuppressWarnings("rawtypes")
                                List<? extends VariableBinding> vbs = event.getResponse().getVariableBindings();
                                // check if sent and received OIDs are the same
                                // or else snmp version may not compatible
                                if (!ScanOIDs.containsValue(((VariableBinding) vbs.get(0)).getOid().toString())) {
                                    // wrong version or not correct response!
                                    return;
                                }
                                NotifyForNewDevice(event.getPeerAddress(), SNMPVersion.V2c,
                                        scannerMakeDescriptionString(parseResponseVectorToHashMap(vbs)));
                            }
                        }
                    }

                    ScanResponseListener listener = new ScanResponseListener();
                    snmp.send(pdu, target, null, listener);
                } // end of community loop

                nextIp = getNextBroadcastIPV4Address(nextIp);
            } // end of IP loop

        } catch (IOException e1) {
            logger.error("", e1);
        }

    }
}
