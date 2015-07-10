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
package org.openmuc.framework.driver.canopen;

import org.openmuc.framework.config.ArgumentSyntaxException;
import org.openmuc.framework.driver.spi.ChannelRecordContainer;
import org.openmuc.jcanopen.datatypes.NumericDataType;
import org.openmuc.jcanopen.pdo.PDOObject;

/**
 * @author Frederic Robra
 * 
 */
public class PDOObjectImpl implements PDOObject, Comparable<PDOObjectImpl> {

	private int cobId;
	private final int position;
	private final short length;
	private NumericDataType numericDataType;
	private byte[] data;
	private final ChannelRecordContainer container;

	public PDOObjectImpl(ChannelRecordContainer container) throws ArgumentSyntaxException {
		this.container = container;
		String channelAddressSyntax = container.getChannelAddress();
		String[] address = channelAddressSyntax.split(":");
		if (address == null || address.length < 4 || !address[0].equals("PDO")) {
			throw new ArgumentSyntaxException("channel is not a pdo");
		}

		cobId = Transforms.parseHexOrDecValue(address[1]);
		position = Transforms.parseHexOrDecValue(address[2]);
		length = (short) Transforms.parseHexOrDecValue(address[3]);
		numericDataType = null;

		if (address.length > 4) {
			numericDataType = Transforms.parseDataType(address[4]);
		}
	}

	private PDOObjectImpl(int position, short length, NumericDataType numericDataType, byte[] data,
			ChannelRecordContainer container) {
		this.position = position;
		this.length = length;
		this.numericDataType = numericDataType;
		this.data = data;
		this.container = container;
	}

	public int getCobId() {
		return cobId;
	}

	@Override
	public short getLength() {
		return length;
	}

	@Override
	public byte[] getData() {
		return data;
	}

	@Override
	public void setData(byte[] data) {
		this.data = data;
	}

	public NumericDataType getNumericDataType() {
		return numericDataType;
	}

	public ChannelRecordContainer getContainer() {
		return container;
	}

	@Override
	public PDOObject copy() {
		byte[] data = null;
		if (this.data != null) {
			data = new byte[this.data.length];
			System.arraycopy(this.data, 0, data, 0, data.length);
		}
		return new PDOObjectImpl(position, length, numericDataType, data, container);
	}

	@Override
	public int compareTo(PDOObjectImpl object) {
		return position - object.position;
	}
}
