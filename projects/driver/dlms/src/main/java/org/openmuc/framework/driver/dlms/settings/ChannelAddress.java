/*
 * Copyright 2011-18 Fraunhofer ISE
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
package org.openmuc.framework.driver.dlms.settings;

import org.openmuc.framework.config.ArgumentSyntaxException;
import org.openmuc.jdlms.AttributeAddress;
import org.openmuc.jdlms.ObisCode;
import org.openmuc.jdlms.datatypes.DataObject.Type;

public class ChannelAddress extends GenericSetting {

    private static final String LOGICAL_NAME_FORMAT = "<Interface_Class_ID>/<Instance_ID>/<Object_Attribute_ID>";

    @Option(value = "a", mandatory = true, range = LOGICAL_NAME_FORMAT)
    private String address;
    @Option(value = "t", range = "DataObject.Type")
    private String type;

    private AttributeAddress attributeAddress;
    private Type dataObjectType;

    public ChannelAddress(String channelAddress) throws ArgumentSyntaxException {
        int optionsNumber = parseFields(channelAddress);

        if (optionsNumber > 2) {
            throw new ArgumentSyntaxException("Too many arguments given.");
        }
        else if (optionsNumber < 1) {
            throw new ArgumentSyntaxException("Attribute address must be provided.");
        }
        try {
            attributeAddress = buildlAttributeAddress(address);
        } catch (NumberFormatException e) {
            throw new ArgumentSyntaxException("Class ID or Attribute ID is not a number.");
        } catch (IllegalArgumentException e) {
            throw new ArgumentSyntaxException(e.getMessage());
        }
        try {

            if (type != null) {
                dataObjectType = typeFrom(type);
            }
        } catch (IllegalArgumentException e) {
            throw new ArgumentSyntaxException("Type of DataObject is unknown.");
        }

    }

    public String getAddress() {
        return address;
    }

    public Type getType() {
        return dataObjectType;
    }

    public AttributeAddress getAttributeAddress() {
        return attributeAddress;
    }

    private static Type typeFrom(String typeAsString) throws IllegalArgumentException {
        return Type.valueOf(typeAsString.toUpperCase().trim());
    }

    private static AttributeAddress buildlAttributeAddress(String requestParameter)
            throws IllegalArgumentException, NumberFormatException {
        String[] arguments = requestParameter.split("/");

        if (arguments.length != 3) {
            String msg = String.format("Wrong number of DLMS/COSEM address arguments. %s", LOGICAL_NAME_FORMAT);
            throw new IllegalArgumentException(msg);
        }

        int classId = Integer.parseInt(arguments[0]);
        ObisCode instanceId = new ObisCode(arguments[1]);
        int attributeId = Integer.parseInt(arguments[2]);

        return new AttributeAddress(classId, instanceId, attributeId);
    }

}
