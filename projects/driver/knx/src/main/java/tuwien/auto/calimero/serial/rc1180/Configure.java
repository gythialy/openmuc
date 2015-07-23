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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.Semaphore;

import tuwien.auto.calimero.DataUnitBuilder;
import tuwien.auto.calimero.exception.KNXException;
import tuwien.auto.calimero.exception.KNXIllegalArgumentException;
import tuwien.auto.calimero.exception.KNXTimeoutException;
import tuwien.auto.calimero.log.LogService;

/**
 * Used to configure the RC1180 chip
 * 
 * @author Frederic Robra
 * 
 */
class Configure {

	private static final byte CONFIG_MODE_ENTER = 0x00;
	private static final byte CONFIG_MODE_EXIT = 0x58;
	private static final byte CONFIG_MODE_GT_PROMPT = 0x3E;

	private static final byte RF_POWER = 0x01;
	private static final byte RF_POWER_ARG_DEFAULT = 0x05;

	private static final byte KNX_MODE = 0x03;
	private static final byte KNX_MODE_ARG_DEFAULT = 0x00;

	private static final byte SLEEP_MODE = 0x04;
	private static final byte SLEEP_MODE_ARG_DEFAULT = 0x00;

	private static final byte RSSI_MODE = 0x05;
	private static final byte RSSI_MODE_ARG_DEFAULT = 0x00;

	private static final byte PREAMBLE_LENGTH = 0x0A;
	// private static final byte PREAMBLE_LENGTH_ARG_DEFAULT = 0x00;
	private static final byte PREAMBLE_LENGTH_ARG_LONG = 0x01;

	private static final byte BATTERY_THRESHOLD = 0x0B;
	private static final byte BATTERY_THRESHOLD_ARG_DEFAULT = 85;
	// private static final byte BATTERY_THRESHOLD_ARG_DISABLE = 0x00;

	private static final byte TIMEOUT = 0x10;
	private static final byte TIMEOUT_ARG_DEFAULT = 0x7C;

	private static final byte NETWORK_ROLE = 0x12;
	private static final byte NETWORK_ROLE_ARG_DEFAULT = 0x00;

	private static final byte DATA_INTERFACE = 0x36;
	// private static final byte DATA_INTERFACE_ARG_DEFAULT = 0x00;
	private static final byte DATA_INTERFACE_ARG_ADDSTARTSTOPBYTE = 0x04;
	// private static final byte DATA_INTERFACE_ARG_TXCOMPLETE = 0x10;

	private static final byte SERIAL_NUMBER_0 = 0x1B;
	private static final byte SERIAL_NUMBER_1 = 0x1C;
	private static final byte SERIAL_NUMBER_2 = 0x1D;
	private static final byte SERIAL_NUMBER_3 = 0x1E;
	private static final byte SERIAL_NUMBER_4 = 0x1F;
	private static final byte SERIAL_NUMBER_5 = 0x20;

	private static final byte DOMAIN_ADDRESS_0 = 0x21;
	private static final byte DOMAIN_ADDRESS_1 = 0x22;
	private static final byte DOMAIN_ADDRESS_2 = 0x23;
	private static final byte DOMAIN_ADDRESS_3 = 0x24;
	private static final byte DOMAIN_ADDRESS_4 = 0x25;
	private static final byte DOMAIN_ADDRESS_5 = 0x26;

	private static final byte CONFIG_MODE_TEST_MODE_0 = 0x30;
	private static final byte CONFIG_MODE_MEMORY_CONFIGURATION = 0x4D;
	private static final byte CONFIG_MODE_MEMORY_CONFIGURATION_EXIT = (byte) 0xFF;

	private final DataOutputStream os;
	private final DataInputStream is;
	private final LogService logger;
	private final Semaphore io;

	/**
	 * Used to configure the RC1180 chip
	 * 
	 * @param is
	 *            UART TDX pin
	 * @param os
	 *            UART RDX pin
	 * @param io
	 *            semaphore used to lock the input and output stream
	 * @param logger
	 */
	public Configure(DataInputStream is, DataOutputStream os, Semaphore io, LogService logger) {
		this.is = is;
		this.os = os;
		this.io = io;
		this.logger = logger;
	}

	/**
	 * Sets the specific settings
	 * 
	 * @throws KNXException
	 */
	public void set() throws KNXException {
		try {
			io.acquireUninterruptibly();
			logger.trace("enter configuration mode for RC1180-KNX1");
			sendAndWaitForResponse(CONFIG_MODE_ENTER, CONFIG_MODE_GT_PROMPT);

			byte[] memory = readMemory();

			configure(memory, RF_POWER, RF_POWER_ARG_DEFAULT, "Default RF output power");
			configure(memory, KNX_MODE, KNX_MODE_ARG_DEFAULT, "Set mode to S2");
			configure(memory, SLEEP_MODE, SLEEP_MODE_ARG_DEFAULT, "Disable sleep mode");
			configure(memory, RSSI_MODE, RSSI_MODE_ARG_DEFAULT, "Do not append RSSI to received data");
			configure(memory, PREAMBLE_LENGTH, PREAMBLE_LENGTH_ARG_LONG, "Long preamble (KNX Ready)");
			configure(memory, BATTERY_THRESHOLD, BATTERY_THRESHOLD_ARG_DEFAULT,
					"Threshold battery voltage (2.5V) for alarm");
			configure(memory, TIMEOUT, TIMEOUT_ARG_DEFAULT, "Modem clears buffer after 2s");
			configure(memory, NETWORK_ROLE, NETWORK_ROLE_ARG_DEFAULT, "Transmitter/Receiver");
			configure(memory, DATA_INTERFACE, (DATA_INTERFACE_ARG_ADDSTARTSTOPBYTE),
					"Add start (68h) and stop (16h) byte");

			logger.trace("exit configuration mode");
			sendByte(CONFIG_MODE_EXIT);
		} catch (KNXException e) {
			throw e;
		} finally {
			io.release();
		}
	}

	/**
	 * Performs a factory reset of the RC1180 chip
	 * 
	 * @throws KNXException
	 */
	public void factoryReset() throws KNXException {
		try {
			io.acquireUninterruptibly();
			logger.info("factory reset...");
			sendAndWaitForResponse(CONFIG_MODE_ENTER, CONFIG_MODE_GT_PROMPT);
			sendAndWaitForResponse(CONFIG_MODE_MEMORY_CONFIGURATION, CONFIG_MODE_GT_PROMPT);

			int[] memory = new int[] { 0x0B, 0x05, 0x02, 0x00, 0x00, 0x00, 0x64, 0x00, 0x05, 0x3C, 0x00, 0x55, 0x00,
					0x00, 0x80, 0x80, 0x7C, 0x00, 0x00, 0x01, 0x00, 0x00, 0x17, 0x00, 0x00, 0xFF, 0x00, 0x12, 0x34,
					0x56, 0x78, 0x90, 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x04, 0xFF, 0x08, 0x00, 0x00, 0x00,
					0x00, 0x00, 0x00, 0x05, 0x08, 0x00, 0x01, 0x05, 0x00, 0x00, 0x01, 0x2B, 0x00, 0x00, 0x44, 0x06,
					0x02, 0x00, 0x00, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF,
					0xFF, 0xFF, 0xFF, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
					0x00, 0x00, 0x00, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
					-1, -1, -1, -1, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF,
					0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF,
					0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF,
					0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF,
					0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF,
					0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF,
					0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF,
					0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF,
					0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF };

			for (int i = 0x00; i < 0x60; i++) {
				sendConfig(i, memory[i]);
			}
			for (int i = 0x78; i <= 0xFF; i++) {
				sendConfig(i, memory[i]);
			}

			sendAndWaitForResponse(CONFIG_MODE_MEMORY_CONFIGURATION_EXIT, CONFIG_MODE_GT_PROMPT);
			sendByte(CONFIG_MODE_EXIT);
		} catch (KNXException e) {
			throw e;
		} finally {
			io.release();
		}
	}

	/**
	 * @return the serial number saved on the chip
	 * @throws KNXException
	 */
	public byte[] getSerialNumber() throws KNXException {
		byte[] serialNumber = new byte[6];
		try {
			io.acquireUninterruptibly();
			sendAndWaitForResponse(CONFIG_MODE_ENTER, CONFIG_MODE_GT_PROMPT);
			byte[] memory = readMemory();
			sendByte(CONFIG_MODE_EXIT);
			serialNumber[0] = memory[SERIAL_NUMBER_0];
			serialNumber[1] = memory[SERIAL_NUMBER_1];
			serialNumber[2] = memory[SERIAL_NUMBER_2];
			serialNumber[3] = memory[SERIAL_NUMBER_3];
			serialNumber[4] = memory[SERIAL_NUMBER_4];
			serialNumber[5] = memory[SERIAL_NUMBER_5];
		} catch (KNXException e) {
			throw e;
		} finally {
			io.release();
		}
		return serialNumber;
	}

	/**
	 * @return the domain address saved on the chip
	 * @throws KNXException
	 */
	public byte[] getDomainAddress() throws KNXException {
		byte[] domainAddress = new byte[6];
		try {
			io.acquireUninterruptibly();
			sendAndWaitForResponse(CONFIG_MODE_ENTER, CONFIG_MODE_GT_PROMPT);
			byte[] memory = readMemory();
			sendByte(CONFIG_MODE_EXIT);
			domainAddress[0] = memory[DOMAIN_ADDRESS_0];
			domainAddress[1] = memory[DOMAIN_ADDRESS_1];
			domainAddress[2] = memory[DOMAIN_ADDRESS_2];
			domainAddress[3] = memory[DOMAIN_ADDRESS_3];
			domainAddress[4] = memory[DOMAIN_ADDRESS_4];
			domainAddress[5] = memory[DOMAIN_ADDRESS_5];
		} catch (KNXException e) {
			throw e;
		} finally {
			io.release();
		}
		return domainAddress;
	}

	/**
	 * Writes the serial number to the chip. A write will only be performed if the address on the chip differs
	 * 
	 * @param serialNumber
	 *            byte array containing the serial number
	 * @throws KNXException
	 */
	public void setSerialNumber(final byte[] serialNumber) throws KNXException {
		if (serialNumber.length != 6) {
			throw new KNXIllegalArgumentException("wrong length of serial number");
		}
		byte[] oldSerialNumber = getSerialNumber();
		if (!Arrays.equals(oldSerialNumber, serialNumber)) {
			try {
				io.acquireUninterruptibly();
				logger.info("setting serial number to " + DataUnitBuilder.toHex(serialNumber, ":"));
				sendAndWaitForResponse(CONFIG_MODE_ENTER, CONFIG_MODE_GT_PROMPT);
				sendAndWaitForResponse(CONFIG_MODE_MEMORY_CONFIGURATION, CONFIG_MODE_GT_PROMPT);

				sendConfig(SERIAL_NUMBER_0, serialNumber[0]);
				sendConfig(SERIAL_NUMBER_1, serialNumber[1]);
				sendConfig(SERIAL_NUMBER_2, serialNumber[2]);
				sendConfig(SERIAL_NUMBER_3, serialNumber[3]);
				sendConfig(SERIAL_NUMBER_4, serialNumber[4]);
				sendConfig(SERIAL_NUMBER_5, serialNumber[5]);

				sendAndWaitForResponse(CONFIG_MODE_MEMORY_CONFIGURATION_EXIT, CONFIG_MODE_GT_PROMPT);
				sendByte(CONFIG_MODE_EXIT);
			} catch (KNXException e) {
				throw e;
			} finally {
				io.release();
			}
		}
	}

	/**
	 * Writes the domain address to the chip. A write will only be performed if the address on the chip differs
	 * 
	 * @param domainAddress
	 *            byte array containing the domain address
	 * @throws KNXException
	 */
	public void setDomainAddress(final byte[] domainAddress) throws KNXException {
		if (domainAddress.length != 6) {
			throw new KNXIllegalArgumentException("wrong length of domain address");
		}
		byte[] oldDomainAddress = getDomainAddress();
		if (!Arrays.equals(oldDomainAddress, domainAddress)) {
			try {
				io.acquireUninterruptibly();
				logger.info("setting domain addres to " + DataUnitBuilder.toHex(domainAddress, ":"));
				sendAndWaitForResponse(CONFIG_MODE_ENTER, CONFIG_MODE_GT_PROMPT);
				sendAndWaitForResponse(CONFIG_MODE_MEMORY_CONFIGURATION, CONFIG_MODE_GT_PROMPT);

				sendConfig(DOMAIN_ADDRESS_0, domainAddress[0]);
				sendConfig(DOMAIN_ADDRESS_1, domainAddress[1]);
				sendConfig(DOMAIN_ADDRESS_2, domainAddress[2]);
				sendConfig(DOMAIN_ADDRESS_3, domainAddress[3]);
				sendConfig(DOMAIN_ADDRESS_4, domainAddress[4]);
				sendConfig(DOMAIN_ADDRESS_5, domainAddress[5]);

				sendAndWaitForResponse(CONFIG_MODE_MEMORY_CONFIGURATION_EXIT, CONFIG_MODE_GT_PROMPT);
				sendByte(CONFIG_MODE_EXIT);
			} catch (KNXException e) {
				throw e;
			} finally {
				io.release();
			}
		}
	}

	private void configure(byte[] memory, byte parameter, byte argument, String description) throws KNXException {
		if (memory[parameter] != argument) {
			sendAndWaitForResponse(CONFIG_MODE_MEMORY_CONFIGURATION, CONFIG_MODE_GT_PROMPT);
			logger.info(description);
			sendConfig(parameter, argument);
			sendAndWaitForResponse(CONFIG_MODE_MEMORY_CONFIGURATION_EXIT, CONFIG_MODE_GT_PROMPT);
		}
	}

	private byte[] readMemory() throws KNXException {
		int length = 257; // length = 0xff + end_prompt ('>')
		byte[] memory = new byte[length];
		sendByte(CONFIG_MODE_TEST_MODE_0);
		int available;
		int read = 0;
		int timeout = 1000;
		do {
			try {
				available = is.available();
				is.read(memory, read, available);
				read += available;
			} catch (IOException e) {
				logger.error(e.getMessage());
				throw new KNXException(e.getMessage(), e);
			}
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
			timeout -= 100;
			if (timeout == 0) {
				throw new KNXTimeoutException("memory read failed");
			}
		} while (read < length);

		return memory;
	}

	private void sendByte(int send) {
		try {
			os.writeByte(send);
			os.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void sendConfig(int command, int argument) throws KNXException {
		// sendAndWaitForResponse(command, CONFIG_MODE_GT_PROMPT);
		// sendAndWaitForResponse(argument, CONFIG_MODE_GT_PROMPT);
		sendByte(command);
		sendByte(argument);
	}

	private void sendAndWaitForResponse(int send, byte response) throws KNXException {
		sendByte(send);
		int timeout = 1000;
		boolean wait = true;
		int available;
		do {
			try {
				available = is.available();
				if (available > 0) {
					byte[] buffer = new byte[available];
					is.read(buffer);
					for (byte b : buffer) {
						if (b == response) {
							wait = false;
							break;
						}
					}
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
					}
					timeout -= 100;
					if (timeout == 0) {
						throw new KNXTimeoutException("configuring failed");
					}
				}
			} catch (IOException e) {
				logger.error(e.getMessage());
				throw new KNXException(e.getMessage(), e);
			}

		} while (wait);

	}
}
