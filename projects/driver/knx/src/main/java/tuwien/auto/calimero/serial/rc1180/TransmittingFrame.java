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

import tuwien.auto.calimero.DataUnitBuilder;
import tuwien.auto.calimero.GroupAddress;
import tuwien.auto.calimero.IndividualAddress;
import tuwien.auto.calimero.KNXAddress;

/**
 * Container for a frame going to a RC1180 chip
 * 
 * @author Frederic Robra
 * 
 */
class TransmittingFrame {

	private byte[] frame;

	/**
	 * Container for a frame going to a RC1180 chip.
	 * <p>
	 * The Linklayer Frame Number will be 0
	 * 
	 * @param src
	 *            source address
	 * @param dst
	 *            destination address
	 * @param hopCount
	 *            repetition counter
	 * @param nsdu
	 *            network layer service data unit
	 */
	public TransmittingFrame(IndividualAddress src, KNXAddress dst, int hopCount, byte[] nsdu) {
		init(src, dst, hopCount, 0, false, nsdu);
	}

	/**
	 * Container for a frame going to a RC1180 chip.
	 * 
	 * @param src
	 *            source address
	 * @param dst
	 *            destination address
	 * @param hopCount
	 *            repetition counter
	 * @param addressExtentionType
	 *            For the Standard Frame, the AET shall be used as follows:<br>
	 *            0: The field SN/DoA in the first block shall be interpreted as the KNX Serial Number of the sender.<br>
	 *            1: The field SN/DoA in the first block shall be interpreted as the RF Domain Address.
	 * @param linkLayerFrameNumber
	 *            Linklayer Frame Number
	 * @param nsdu
	 *            network layer service data unit
	 */
	public TransmittingFrame(IndividualAddress src, KNXAddress dst, int hopCount, int linkLayerFrameNumber,
			boolean addressExtentionType, byte[] nsdu) {
		init(src, dst, hopCount, linkLayerFrameNumber, addressExtentionType, nsdu);
	}

	private void init(IndividualAddress src, KNXAddress dst, int hopCount, int linkLayerFrameNumber,
			boolean addressExtentionType, byte[] nsdu) {
		frame = new byte[nsdu.length + 7];

		frame[0] = (byte) (frame.length - 1);
		if (nsdu.length > 16) { // Extended frame format
			frame[1] = 0x04;
		}
		else {
			frame[1] = 0x00;
		}

		frame[2] = (byte) (src.getRawAddress() >> 8);
		frame[3] = (byte) src.getRawAddress();

		frame[4] = (byte) (dst.getRawAddress() >> 8);
		frame[5] = (byte) dst.getRawAddress();

		frame[6] = 0;
		if (dst instanceof GroupAddress) {
			frame[6] = (byte) 0x80;
		}
		frame[6] |= hopCount << 4;
		frame[6] |= addressExtentionType ? 1 : 0; // chip uses domain address or serial number

		frame[6] |= linkLayerFrameNumber << 1;

		System.arraycopy(nsdu, 0, frame, 7, nsdu.length);

	}

	/**
	 * @return byte array containing the raw frame
	 */
	public byte[] getFrame() {
		return frame;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "frame = " + DataUnitBuilder.toHex(frame, ":");
	}

}
