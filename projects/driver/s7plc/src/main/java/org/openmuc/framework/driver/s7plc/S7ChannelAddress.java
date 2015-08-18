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
package org.openmuc.framework.driver.s7plc;

import com.libnodave.Connection;

/**
 * 
 * Parse an object locator for the Siemens S7 PLC driver
 * 
 * Object locator format:
 * 
 * DB&lt;data block no&gt;.&lt;byte offset&gt;.&lt;data type&gt;
 * 
 * examples DB20.2.uint16 means data block 20, byte offset 2, variable type 16bit unsigned integer
 * 
 * DB10.10.float maps to a REAL variable
 * 
 * @author mzillgit
 * 
 */
public class S7ChannelAddress {

	public static final byte TYPE_BIT = 0;
	public static final byte TYPE_INT8 = 1;
	public static final byte TYPE_UINT8 = 2;
	public static final byte TYPE_INT16 = 3;
	public static final byte TYPE_UINT16 = 4;
	public static final byte TYPE_INT32 = 5;
	public static final byte TYPE_UINT32 = 6;
	public static final byte TYPE_INT64 = 7;
	public static final byte TYPE_UINT64 = 8;
	public static final byte TYPE_FLOAT = 9;
	public static final byte TYPE_DOUBLE = 10;

	private int memoryArea;
	private int areaAddress;
	private int offset;
	private byte type;
	private byte bitPos;

	public S7ChannelAddress(String locator) throws MalformendObjectLocatorException {
		int areaAddressPos = 0;

		if (locator.startsWith("DB")) {
			memoryArea = Connection.AREA_DB;
			areaAddressPos = 2;
		}
		else {
			throw new MalformendObjectLocatorException("Unknown area");
		}

		int offsetPos = locator.indexOf('.');
		try {
			areaAddress = Integer.parseInt(locator.substring(areaAddressPos, offsetPos));
		} catch (NumberFormatException e) {
			throw new MalformendObjectLocatorException("NumberFormatException in areaAddress");
		}

		int typePos = locator.indexOf(':');
		try {
			offset = Integer.parseInt(locator.substring(offsetPos + 1, typePos));
		} catch (NumberFormatException e) {
			throw new MalformendObjectLocatorException("NumberFormatException in offset");
		}

		String typeStr = locator.substring(typePos + 1);

		if (typeStr.equals("float")) {
			type = TYPE_FLOAT;
		}
		else if (typeStr.equals("uint8")) {
			type = TYPE_UINT8;
		}
		else if (typeStr.equals("int8")) {
			type = TYPE_INT8;
		}
		else if (typeStr.equals("uint16")) {
			type = TYPE_UINT16;
		}
		else if (typeStr.equals("int16")) {
			type = TYPE_INT16;
		}
		else if (typeStr.equals("uint32")) {
			type = TYPE_UINT32;
		}
		else if (typeStr.equals("int32")) {
			type = TYPE_INT32;
		}
		else if (typeStr.equals("double")) {
			type = TYPE_DOUBLE;
		}
		else if (typeStr.startsWith("bit(")) {
			type = TYPE_BIT;
			String bitPos = locator.substring(typePos + 5, typePos + 6);
			try {
				this.bitPos = Byte.parseByte(bitPos);
			} catch (Exception e) {
				throw new MalformendObjectLocatorException("Invalid bit locator");
			}
		}
		else {
			throw new MalformendObjectLocatorException("Unknown or missing type");
		}
	}

	public int getMemoryArea() {
		return memoryArea;
	}

	public int getAreaAddress() {
		return areaAddress;
	}

	public int getOffset() {
		return offset;
	}

	public int getDataLength() {

		switch (type) {
		case TYPE_BIT:
		case TYPE_UINT8:
		case TYPE_INT8:
			return 1;
		case TYPE_UINT16:
		case TYPE_INT16:
			return 2;
		case TYPE_UINT32:
		case TYPE_INT32:
		case TYPE_FLOAT:
			return 4;
		case TYPE_UINT64:
		case TYPE_INT64:
		case TYPE_DOUBLE:
			return 8;
		default:
			return -1;
		}
	}

	public byte getType() {
		return type;
	}

	public byte getBitPos() {
		return bitPos;
	}

	// public static void main(String[] args) {
	// try {
	// S7ObjectLocator loc = new S7ObjectLocator("DB3.0:float");
	// System.out.println("Area DB? " + loc.getAreaAddress() + "." +
	// loc.getOffset() + " type:" + loc.getType());
	//
	// loc = new S7ObjectLocator("DB3.123:uint16");
	// System.out.println("Area DB? " + loc.getAreaAddress() + "." +
	// loc.getOffset() + " type:" + loc.getType());
	//
	// loc = new S7ObjectLocator("DB18.24:double");
	// System.out.println("Area DB? " + loc.getAreaAddress() + "." +
	// loc.getOffset() + " type:" + loc.getType());
	// } catch (MalformendObjectLocatorException e) {
	// e.printStackTrace();
	// }
	// }

}
