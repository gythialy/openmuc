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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.openmuc.framework.config.ChannelScanInfo;
import org.openmuc.framework.data.ByteValue;
import org.openmuc.framework.data.DoubleValue;
import org.openmuc.framework.data.Flag;
import org.openmuc.framework.data.IntValue;
import org.openmuc.framework.data.LongValue;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.data.ShortValue;
import org.openmuc.framework.data.Value;
import org.openmuc.framework.driver.spi.ChannelRecordContainer;
import org.openmuc.framework.driver.spi.ChannelValueContainer;
import org.openmuc.framework.driver.spi.ConnectionException;
import org.openmuc.framework.driver.spi.RecordsReceivedListener;

import com.libnodave.Connection;
import com.libnodave.Interface;

public class S7PlcConnection implements org.openmuc.framework.driver.spi.Connection {

	private final Connection connection;
	private final Interface ifc;

	public S7PlcConnection(Connection connection, Interface ifc) {
		this.connection = connection;
		this.ifc = ifc;
	}

	@Override
	public List<ChannelScanInfo> scanForChannels(String settings) throws UnsupportedOperationException,
			ConnectionException {
		throw new UnsupportedOperationException();
	}

	private class DataBlock {
		int nr;
		int maxOffset = 0;
		byte[] buf = null;

		public DataBlock(int nr) {
			this.nr = nr;
		}
	}

	private class DataObject {
		ChannelRecordContainer container;
		S7ChannelAddress channelAddress;

		public DataObject(ChannelRecordContainer container, S7ChannelAddress channelAddress) {
			this.container = container;
			this.channelAddress = channelAddress;
		}
	}

	private byte[] concat(byte[] A, byte[] B) {
		byte[] C = new byte[A.length + B.length];
		System.arraycopy(A, 0, C, 0, A.length);
		System.arraycopy(B, 0, C, A.length, B.length);

		return C;
	}

	private int getBit(byte b, byte bitPos) {
		switch (bitPos) {
		case 0:
			if ((b & 0x01) != 0) {
				return 1;
			}
			else {
				return 0;
			}
		case 1:
			if ((b & 0x02) != 0) {
				return 1;
			}
			else {
				return 0;
			}
		case 2:
			if ((b & 0x04) != 0) {
				return 1;
			}
			else {
				return 0;
			}
		case 3:
			if ((b & 0x08) != 0) {
				return 1;
			}
			else {
				return 0;
			}
		case 4:
			if ((b & 0x10) != 0) {
				return 1;
			}
			else {
				return 0;
			}
		case 5:
			if ((b & 0x20) != 0) {
				return 1;
			}
			else {
				return 0;
			}
		case 6:
			if ((b & 0x40) != 0) {
				return 1;
			}
			else {
				return 0;
			}
		case 7:
			if ((b & 0x80) != 0) {
				return 1;
			}
			else {
				return 0;
			}
		default:
			return 0;
		}
	}

	@Override
	public Object read(List<ChannelRecordContainer> containers, Object containerListHandle, String samplingGroup)
			throws UnsupportedOperationException, ConnectionException {

		HashMap<Integer, DataBlock> dbList = new HashMap<Integer, DataBlock>();
		List<DataObject> objList = new LinkedList<DataObject>();

		for (ChannelRecordContainer container : containers) {
			S7ChannelAddress channelAddress;
			try {
				channelAddress = new S7ChannelAddress(container.getChannelAddress());

			} catch (MalformendObjectLocatorException e) {
				container.setRecord(new Record(Flag.DRIVER_ERROR_CHANNEL_ADDRESS_SYNTAX_INVALID));
				continue;
			}

			int dbAddr = channelAddress.getAreaAddress();
			DataBlock db;

			if (!dbList.containsKey(dbAddr)) {
				dbList.put(dbAddr, new DataBlock(dbAddr));
			}

			db = dbList.get(dbAddr);

			int size = channelAddress.getOffset() + channelAddress.getDataLength();

			if (size > db.maxOffset) {
				db.maxOffset = size;
			}

			objList.add(new DataObject(container, channelAddress));

		}

		/* Read out data blocks */
		for (DataBlock db : dbList.values()) {

			try {

				int maxOffset = db.maxOffset;

				int startOffset = 0;

				while (maxOffset > 460) {
					if (db.buf == null) {
						db.buf = connection.readBytes(Connection.AREA_DB, db.nr, startOffset, 460);
					}
					else {
						db.buf = concat(db.buf, connection.readBytes(Connection.AREA_DB, db.nr, startOffset, 460));
					}
					startOffset += 460;
					maxOffset -= 460;
				}

				if (db.buf == null) {
					db.buf = connection.readBytes(Connection.AREA_DB, db.nr, startOffset, maxOffset);
				}
				else {
					db.buf = concat(db.buf, connection.readBytes(Connection.AREA_DB, db.nr, startOffset, maxOffset));
				}

			} catch (IOException e) {
				ifc.close();
				throw new ConnectionException("plcs7: readout error - close socket");
			}
		}

		/* Get data out of buffers */
		for (DataObject obj : objList) {
			DataBlock db = dbList.get(obj.channelAddress.getAreaAddress());

			if (db.buf != null) {
				Value val = null;
				ByteBuffer bbuf;
				bbuf = ByteBuffer.wrap(db.buf);
				bbuf.order(ByteOrder.BIG_ENDIAN);

				switch (obj.channelAddress.getType()) {
				case S7ChannelAddress.TYPE_FLOAT:
					val = new DoubleValue(bbuf.getFloat(obj.channelAddress.getOffset()));
					break;
				case S7ChannelAddress.TYPE_UINT32:
					val = new LongValue(((long) 0xffffffff & (long) bbuf.getInt(obj.channelAddress.getOffset())));
					break;
				case S7ChannelAddress.TYPE_UINT16:
					val = new IntValue((0xffff & bbuf.getShort(obj.channelAddress.getOffset())));
					break;
				case S7ChannelAddress.TYPE_UINT8:
					val = new ShortValue((short) (0xff & bbuf.get(obj.channelAddress.getOffset())));
					break;
				case S7ChannelAddress.TYPE_BIT:
					val = new ByteValue((byte) getBit(bbuf.get(obj.channelAddress.getOffset()),
							obj.channelAddress.getBitPos()));
					break;
				}

				obj.container.setRecord(new Record(val, System.currentTimeMillis()));

			}
			else {
				obj.container.setRecord(new Record(Flag.UNKNOWN_ERROR));
			}

		}
		return null;
	}

	@Override
	public void startListening(List<ChannelRecordContainer> containers, RecordsReceivedListener listener)
			throws UnsupportedOperationException, ConnectionException {
		// TODO Auto-generated method stub

	}

	@Override
	public Object write(List<ChannelValueContainer> containers, Object containerListHandle)
			throws UnsupportedOperationException, ConnectionException {

		for (ChannelValueContainer container : containers) {

			try {
				S7ChannelAddress locator;
				try {
					locator = new S7ChannelAddress(container.getChannelAddress());
				} catch (MalformendObjectLocatorException e) {
					container.setFlag(Flag.DRIVER_ERROR_CHANNEL_ADDRESS_SYNTAX_INVALID);
					continue;
				}

				byte[] buf;

				switch (locator.getType()) {

				case S7ChannelAddress.TYPE_BIT:
					int intVal = container.getValue().asInt();
					if (intVal == 0) {
						connection.clrBit(locator.getMemoryArea(), locator.getAreaAddress(), locator.getOffset(),
								locator.getBitPos());
					}
					else {
						connection.setBit(locator.getMemoryArea(), locator.getAreaAddress(), locator.getOffset(),
								locator.getBitPos());
					}
					break;

				case S7ChannelAddress.TYPE_FLOAT:
					buf = new byte[4];
					ByteBuffer bbuf = ByteBuffer.wrap(buf);
					bbuf.order(ByteOrder.BIG_ENDIAN);
					bbuf.putFloat(0, container.getValue().asFloat());
					connection.writeBytes(locator.getMemoryArea(), locator.getAreaAddress(), locator.getOffset(), 4,
							buf);
					break;

				case S7ChannelAddress.TYPE_UINT8:
					buf = new byte[1];
					buf[0] = (byte) ((container.getValue().asInt()) & 0xff);
					connection.writeBytes(locator.getMemoryArea(), locator.getAreaAddress(), locator.getOffset(), 1,
							buf);
					break;

				case S7ChannelAddress.TYPE_UINT16:
					buf = new byte[2];
					buf[0] = (byte) ((container.getValue().asInt()) / 0xff);
					buf[1] = (byte) ((container.getValue().asInt()) & 0xff);

					System.out.printf("buf[0]:%02x buf[1]:%02x", buf[0], buf[1]);
					connection.writeBytes(locator.getMemoryArea(), locator.getAreaAddress(), locator.getOffset(), 2,
							buf);
					break;
				}

			} catch (IOException e) {
				// TODO socket close
				throw new ConnectionException();
			}

			container.setFlag(Flag.VALID);

		}
		return null;
	}

	@Override
	public void disconnect() {
		// TODO Auto-generated method stub

	}

}
