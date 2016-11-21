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
package org.openmuc.framework.driver.snmp.implementation;

import java.util.EventObject;

import org.openmuc.framework.driver.snmp.implementation.SnmpDevice.SNMPVersion;
import org.snmp4j.smi.Address;

/**
 * 
 * @author Mehran Shakeri
 * 
 */
public class SnmpDiscoveryEvent extends EventObject {

    /**
     * 
     */
    private static final long serialVersionUID = 1382183246520560859L;
    private final Address deviceAddress;
    private final SNMPVersion snmpVersion;
    private final String description;

    public SnmpDiscoveryEvent(Object source, Address address, SNMPVersion version, String description) {
        super(source);
        deviceAddress = address;
        snmpVersion = version;
        this.description = description;
    }

    public Address getDeviceAddress() {
        return deviceAddress;
    }

    public SNMPVersion getSnmpVersion() {
        return snmpVersion;
    }

    public String getDescription() {
        return description;
    }

}
