/*
 * Copyright 2011-2021 Fraunhofer ISE
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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.openmuc.framework.config.ArgumentSyntaxException;
import org.openmuc.framework.config.ChannelScanInfo;
import org.openmuc.framework.data.ByteArrayValue;
import org.openmuc.framework.data.Flag;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.driver.spi.ChannelRecordContainer;
import org.openmuc.framework.driver.spi.ChannelValueContainer;
import org.openmuc.framework.driver.spi.Connection;
import org.openmuc.framework.driver.spi.ConnectionException;
import org.openmuc.framework.driver.spi.RecordsReceivedListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.AbstractTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.MPv3;
import org.snmp4j.security.SecurityModels;
import org.snmp4j.security.SecurityProtocols;
import org.snmp4j.security.USM;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;

/**
 * 
 * Super class for defining SNMP enabled devices.
 */
public abstract class SnmpDevice implements Connection {

    private static final Logger logger = LoggerFactory.getLogger(SnmpDevice.class);

    public enum SNMPVersion {
        V1,
        V2c,
        V3
    };

    protected Address targetAddress;
    protected Snmp snmp;
    protected USM usm;
    protected int timeout = 3000; // in milliseconds
    protected int retries = 3;
    protected String authenticationPassphrase;
    protected AbstractTarget target;

    protected List<SnmpDiscoveryListener> listeners = new ArrayList<>();

    protected static final Map<String, String> ScanOIDs = new HashMap<>();

    static {
        // some general OIDs that are valid in almost every MIB
        ScanOIDs.put("Device name: ", "1.3.6.1.2.1.1.5.0");
        ScanOIDs.put("Description: ", "1.3.6.1.2.1.1.1.0");
        ScanOIDs.put("Location: ", "1.3.6.1.2.1.1.6.0");
    };

    /**
     * snmp constructor takes primary parameters in order to create snmp object. this implementation uses UDP protocol
     * 
     * @param address
     *            Contains ip and port. accepted string "X.X.X.X/portNo"
     * @param authenticationPassphrase
     *            the authentication pass phrase. If not <code>null</code>, <code>authenticationProtocol</code> must
     *            also be not <code>null</code>. RFC3414 &sect;11.2 requires pass phrases to have a minimum length of 8
     *            bytes. If the length of <code>authenticationPassphrase</code> is less than 8 bytes an
     *            <code>IllegalArgumentException</code> is thrown. [required by snmp4j library]
     * 
     * @throws ConnectionException
     *             thrown if SNMP listen or initialization failed
     * @throws ArgumentSyntaxException
     *             thrown if Device address foramt is wrong
     */
    public SnmpDevice(String address, String authenticationPassphrase)
            throws ConnectionException, ArgumentSyntaxException {

        // start snmp compatible with all versions
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

        // set address
        try {
            targetAddress = GenericAddress.parse(address);
        } catch (IllegalArgumentException e) {
            throw new ArgumentSyntaxException("Device address foramt is wrong! (eg. 1.1.1.1/1)");
        }

        this.authenticationPassphrase = authenticationPassphrase;

    }

    /**
     * Default constructor useful for scanner
     */
    public SnmpDevice() {
    }

    /**
     * set target parameters. Implementations are different in SNMP v1, v2c and v3
     */
    abstract void setTarget();

    /**
     * Receives a list of all OIDs in string format, creates PDU and sends GET request to defined target. This method is
     * a blocking method. It waits for response.
     * 
     * @param OIDs
     *            list of OIDs that should be read from target
     * @return Map&lt;String, String&gt; returns a Map of OID as Key and received value corresponding to that OID from
     *         the target as Value
     * 
     * @throws SnmpTimeoutException
     *             thrown if Target doesn't responses
     * @throws ConnectionException
     *             thrown if SNMP get request fails
     */
    public Map<String, String> getRequestsList(List<String> OIDs) throws SnmpTimeoutException, ConnectionException {

        Map<String, String> result = new HashMap<>();

        // set PDU
        PDU pdu = new PDU();
        pdu.setType(PDU.GET);

        for (String oid : OIDs) {
            pdu.add(new VariableBinding(new OID(oid)));
        }

        // send GET request
        ResponseEvent response;
        try {
            response = snmp.send(pdu, target);
            PDU responsePDU = response.getResponse();
            @SuppressWarnings("rawtypes")
            List<? extends VariableBinding> vbs = responsePDU.getVariableBindings();
            for (int i = 0; i < vbs.size(); i++) {
                VariableBinding vb = vbs.get(i);
                result.put(vb.getOid().toString(), vb.getVariable().toString());
            }
        } catch (IOException e) {
            throw new ConnectionException("SNMP get request failed! " + e.getMessage());
        } catch (NullPointerException e) {
            throw new SnmpTimeoutException("Timeout: Target doesn't respond!");
        }

        return result;
    }

    /**
     * Receives one single OID in string format, creates PDU and sends GET request to defined target. This method is a
     * blocking method. It waits for response.
     * 
     * @param OID
     *            OID that should be read from target
     * @return String containing read value
     * 
     * @throws SnmpTimeoutException
     *             thrown if Target doesn't responses
     * @throws ConnectionException
     *             thrown if SNMP get request failsn
     */
    public String getSingleRequests(String OID) throws SnmpTimeoutException, ConnectionException {

        String result = null;

        // set PDU
        PDU pdu = new PDU();
        pdu.setType(PDU.GET);

        pdu.add(new VariableBinding(new OID(OID)));

        // send GET request
        ResponseEvent response;
        try {
            response = snmp.send(pdu, target);
            PDU responsePDU = response.getResponse();
            @SuppressWarnings("rawtypes")
            List<? extends VariableBinding> vbs = responsePDU.getVariableBindings();
            result = ((VariableBinding) vbs.get(0)).getVariable().toString();
        } catch (IOException e) {
            throw new ConnectionException("SNMP get request failed! " + e.getMessage());
        } catch (NullPointerException e) {
            throw new SnmpTimeoutException("Timeout: Target doesn't respond!");
        }

        return result;
    }

    public String getDeviceAddress() {
        return targetAddress.toString();
    }

    public synchronized void addEventListener(SnmpDiscoveryListener listener) {
        listeners.add(listener);
    }

    public synchronized void removeEventListener(SnmpDiscoveryListener listener) {
        listeners.remove(listener);
    }

    /**
     * This method will call all listeners for given new device
     * 
     * @param address
     *            address of device
     * @param version
     *            version of snmp that this device support
     * @param description
     *            other extra information which can be useful
     * 
     */
    protected synchronized void NotifyForNewDevice(Address address, SNMPVersion version, String description) {
        SnmpDiscoveryEvent event = new SnmpDiscoveryEvent(this, address, version, description);
        @SuppressWarnings("rawtypes")
        Iterator i = listeners.iterator();
        while (i.hasNext()) {
            ((SnmpDiscoveryListener) i.next()).onNewDeviceFound(event);
        }
    }

    /**
     * Calculate and return next broadcast address. (eg. if ip=1.2.3.x, returns 1.2.4.255)
     * 
     * @param ip
     *            IP
     * @return String the next broadcast address as String
     */
    public static String getNextBroadcastIPV4Address(String ip) {
        String[] nums = ip.split("\\.");
        int i = (Integer.parseInt(nums[0]) << 24 | Integer.parseInt(nums[2]) << 8 | Integer.parseInt(nums[1]) << 16
                | Integer.parseInt(nums[3])) + 256;

        return String.format("%d.%d.%d.%d", i >>> 24 & 0xFF, i >> 16 & 0xFF, i >> 8 & 0xFF, 255);
    }

    /**
     * Helper function in order to parse response vector to map structure
     * 
     * @param responseVector
     *            response vector
     * @return HashMap&lt;String, String&gt;
     */
    public static HashMap<String, String> parseResponseVectorToHashMap(List<? extends VariableBinding> responseVector) {

        HashMap<String, String> map = new HashMap<>();
        for (VariableBinding elem : responseVector) {
            map.put(elem.getOid().toString(), elem.getVariable().toString());
        }
        return map;
    }

    protected static String scannerMakeDescriptionString(HashMap<String, String> scannerResult) {

        StringBuilder desc = new StringBuilder();
        for (String key : ScanOIDs.keySet()) {
            desc.append('[')
                    .append(key)
                    .append('(')
                    .append(ScanOIDs.get(key))
                    .append(")=")
                    .append(scannerResult.get(ScanOIDs.get(key)))
                    .append("] ");
        }
        return desc.toString();
    }

    /**
     * Returns respective SNMPVersion enum value based on given SnmpConstant version value
     * 
     * @param version
     *            the version as int
     * @return SNMPVersion or null if given value is not valid
     */
    protected static SNMPVersion getSnmpVersionFromSnmpConstantsValue(int version) {
        switch (version) {
        case 0:
            return SNMPVersion.V1;
        case 1:
            return SNMPVersion.V2c;
        case 3:
            return SNMPVersion.V3;
        }
        return null;
    }

    @Override
    public void disconnect() {
    }

    /**
     * At least device address and channel address must be specified in the container.<br>
     * <br>
     * containers.deviceAddress = device address (eg. 1.1.1.1/161) <br>
     * containers.channelAddress = OID (eg. 1.3.6.1.2.1.1.0)
     * 
     */
    @Override
    public Object read(List<ChannelRecordContainer> containers, Object containerListHandle, String samplingGroup)
            throws ConnectionException {

        return readChannelGroup(containers, timeout);
    }

    @Override
    public Object write(List<ChannelValueContainer> containers, Object containerListHandle)
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
    private Object readChannelGroup(List<ChannelRecordContainer> containers, int timeout) throws ConnectionException {

        new Date().getTime();

        List<String> oids = new ArrayList<>();

        for (ChannelRecordContainer container : containers) {
            if (getDeviceAddress().equalsIgnoreCase(container.getChannel().getDeviceAddress())) {
                oids.add(container.getChannelAddress());
            }
        }

        Map<String, String> values;

        try {
            values = getRequestsList(oids);
            long receiveTime = System.currentTimeMillis();

            for (ChannelRecordContainer container : containers) {
                // make sure the value exists for corresponding channel
                if (values.get(container.getChannelAddress()) != null) {
                    logger.debug("{}: value = '{}'", container.getChannelAddress(),
                            values.get(container.getChannelAddress()));
                    container.setRecord(new Record(
                            new ByteArrayValue(values.get(container.getChannelAddress()).getBytes()), receiveTime));
                }
            }
        } catch (SnmpTimeoutException e) {
            for (ChannelRecordContainer container : containers) {
                container.setRecord(new Record(Flag.TIMEOUT));
            }
        }

        return null;
    }

    @Override
    public void startListening(List<ChannelRecordContainer> containers, RecordsReceivedListener listener)
            throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ChannelScanInfo> scanForChannels(String settings)
            throws UnsupportedOperationException, ConnectionException {
        throw new UnsupportedOperationException();
    }

}
