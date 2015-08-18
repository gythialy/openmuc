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
package tuwien.auto.calimero.serial.rc1180;

import tuwien.auto.calimero.GroupAddress;
import tuwien.auto.calimero.IndividualAddress;
import tuwien.auto.calimero.KNXAddress;
import tuwien.auto.calimero.Priority;
import tuwien.auto.calimero.cemi.CEMILData;
import tuwien.auto.calimero.cemi.CEMILDataEx;
import tuwien.auto.calimero.exception.KNXFormatException;

/**
 * Container for a frame coming from a RC1180 chip
 * 
 * @author Frederic Robra
 * 
 */
class ReceivingFrame {

	private final byte KNXCtrl;
	private final IndividualAddress srcAddress;
	private KNXAddress dstAddress;
	private final byte[] tpdu;
	private final int hopCount;
	private final int linkLayerFrameNumber;
	private final boolean addressExtensionType;
	private final byte[] address;

	/**
	 * Container for a frame coming from a RC1180 chip
	 * 
	 * @param message
	 *            byte array containing the raw message
	 * @throws KNXFormatException
	 */
	public ReceivingFrame(byte[] message) throws KNXFormatException {
		byte length = message[0];
		if (length != message.length - 1) {
			throw new KNXFormatException("wrong length of message");
		}
		byte C = message[1];
		if (C != (byte) 0x44) {
			throw new KNXFormatException("wrong C field");
		}
		byte Esc = message[2];
		if (Esc != (byte) 0xFF) {
			throw new KNXFormatException("wrong Esc field");
		}

		address = new byte[6];
		System.arraycopy(message, 4, address, 0, 6);
		addressExtensionType = (message[15] & 0x01) == 0 ? false : true;

		KNXCtrl = message[10];
		byte[] srcAddress = new byte[2];
		System.arraycopy(message, 11, srcAddress, 0, 2);
		this.srcAddress = new IndividualAddress(srcAddress);

		byte[] dstAddress = new byte[2];
		System.arraycopy(message, 13, dstAddress, 0, 2);
		if (((message[15] >> 7) & 1) > 0) {
			this.dstAddress = new GroupAddress(dstAddress);
		}
		else {
			this.dstAddress = new IndividualAddress(dstAddress);
		}

		hopCount = (message[15] >> 4) & 0x07;
		linkLayerFrameNumber = (message[15] >> 1) & 0x07;

		tpdu = new byte[message.length - 16];
		System.arraycopy(message, 16, tpdu, 0, tpdu.length);
	}

	/**
	 * @return Linklayer Frame Number
	 */
	public int getLinkLayerFrameNumber() {
		return linkLayerFrameNumber;
	}

	/**
	 * @return destination address
	 */
	public KNXAddress getDstAddress() {
		return dstAddress;
	}

	/**
	 * @return calimero cemil data
	 */
	public CEMILData getCemilData() {
		if (KNXCtrl == 0x00) {
			return new CEMILData(CEMILData.MC_LDATA_IND, srcAddress, dstAddress, tpdu, Priority.NORMAL, true, hopCount);
		}
		else { // Extended frame format
			return new CEMILDataEx(CEMILData.MC_LDATA_IND, srcAddress, dstAddress, tpdu, Priority.NORMAL, true,
					hopCount);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return getCemilData().toString();
	}

	/**
	 * @return
	 */
	public Boolean getAET() {
		return addressExtensionType;
	}

	/**
	 * @return
	 */
	public byte[] getSNorDoA() {
		return address;
	}
}
