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
package org.openmuc.framework.driver.modbustcp.test;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openmuc.framework.driver.modbus.EDatatype;
import org.openmuc.framework.driver.modbus.EPrimaryTable;
import org.openmuc.framework.driver.modbus.ModbusChannel;
import org.openmuc.framework.driver.modbus.ModbusChannel.EAccess;

/**
 * This test class tests various parameter combination of the channel address
 * 
 * @author Marco Mittelsdorf
 * 
 */
public class ModbusTcpChannelTest {

    private ArrayList<String> validAddressCombinations;

    @BeforeEach
    public void setUp() {
        validAddressCombinations = new ArrayList<>();

        validAddressCombinations.add("READ:COILS:BOOLEAN");
        validAddressCombinations.add("READ:DISCRETE_INPUTS:BOOLEAN");

        validAddressCombinations.add("READ:HOLDING_REGISTERS:SHORT");
        validAddressCombinations.add("READ:HOLDING_REGISTERS:INT16");
        validAddressCombinations.add("READ:HOLDING_REGISTERS:FLOAT");
        validAddressCombinations.add("READ:HOLDING_REGISTERS:DOUBLE");
        validAddressCombinations.add("READ:HOLDING_REGISTERS:LONG");

        validAddressCombinations.add("READ:INPUT_REGISTERS:SHORT");
        validAddressCombinations.add("READ:INPUT_REGISTERS:INT16");
        validAddressCombinations.add("READ:INPUT_REGISTERS:FLOAT");
        validAddressCombinations.add("READ:INPUT_REGISTERS:DOUBLE");
        validAddressCombinations.add("READ:INPUT_REGISTERS:LONG");

        validAddressCombinations.add("WRITE:COILS:BOOLEAN");
        validAddressCombinations.add("WRITE:HOLDING_REGISTERS:SHORT");
        validAddressCombinations.add("WRITE:HOLDING_REGISTERS:INT16");
        validAddressCombinations.add("WRITE:HOLDING_REGISTERS:FLOAT");
        validAddressCombinations.add("WRITE:HOLDING_REGISTERS:DOUBLE");
        validAddressCombinations.add("WRITE:HOLDING_REGISTERS:LONG");

    }

    @Test
    public void testValidReadAddresses() {

        ArrayList<String> validAddresses = new ArrayList<>();

        validAddresses.add("0:DISCRETE_INPUTS:0:BOOLEAN");
        validAddresses.add("0:COILS:0:BOOLEAN");

        validAddresses.add("0:INPUT_REGISTERS:0:SHORT");
        validAddresses.add("0:INPUT_REGISTERS:0:INT16");
        validAddresses.add("0:INPUT_REGISTERS:0:FLOAT");
        validAddresses.add("0:INPUT_REGISTERS:0:DOUBLE");
        validAddresses.add("0:INPUT_REGISTERS:0:LONG");

        validAddresses.add("0:HOLDING_REGISTERS:0:SHORT");
        validAddresses.add("0:HOLDING_REGISTERS:0:INT16");
        validAddresses.add("0:HOLDING_REGISTERS:0:FLOAT");
        validAddresses.add("0:HOLDING_REGISTERS:0:DOUBLE");
        validAddresses.add("0:HOLDING_REGISTERS:0:LONG");

        for (String channelAddress : validAddresses) {
            try {
                ModbusChannel channel = new ModbusChannel(channelAddress, EAccess.READ);
                String testString = concatenate(channel.getAccessFlag(), channel.getPrimaryTable(),
                        channel.getDatatype());
                if (!validAddressCombinations.contains(testString.toUpperCase())) {
                    fail(testString + "is not a valid paramaeter combination");
                }
                else {
                    System.out.println(channelAddress + " and resulting " + testString.toUpperCase() + " are valid.");
                }

            } catch (Exception e) {
                e.printStackTrace();
                fail("unexpected exception");
            }
        }
    }

    @Test
    public void testValidWriteAddresses() {

        ArrayList<String> validAddresses = new ArrayList<>();

        validAddresses.add("0:COILS:0:BOOLEAN");

        validAddresses.add("0:HOLDING_REGISTERS:0:SHORT");
        validAddresses.add("0:HOLDING_REGISTERS:0:INT16");
        validAddresses.add("0:HOLDING_REGISTERS:0:FLOAT");
        validAddresses.add("0:HOLDING_REGISTERS:0:DOUBLE");
        validAddresses.add("0:HOLDING_REGISTERS:0:LONG");

        for (String channelAddress : validAddresses) {
            try {
                ModbusChannel channel = new ModbusChannel(channelAddress, EAccess.WRITE);
                String testString = concatenate(channel.getAccessFlag(), channel.getPrimaryTable(),
                        channel.getDatatype());
                if (!validAddressCombinations.contains(testString.toUpperCase())) {
                    fail(testString + "is not a valid paramaeter combination");
                }
                else {
                    System.out.println(channelAddress + " and resulting " + testString.toUpperCase() + " are valid.");
                }
            } catch (Exception e) {
                e.printStackTrace();
                fail("unexpected exception");
            }
        }
    }

    private String concatenate(EAccess a, EPrimaryTable p, EDatatype d) {
        return a.toString() + ":" + p.toString() + ":" + d.toString();
    }

}
