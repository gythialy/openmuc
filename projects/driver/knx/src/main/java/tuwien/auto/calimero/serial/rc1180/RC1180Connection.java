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

import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.EventListener;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;

import tuwien.auto.calimero.DataUnitBuilder;
import tuwien.auto.calimero.FrameEvent;
import tuwien.auto.calimero.IndividualAddress;
import tuwien.auto.calimero.KNXAddress;
import tuwien.auto.calimero.KNXListener;
import tuwien.auto.calimero.exception.KNXException;
import tuwien.auto.calimero.internal.EventListeners;
import tuwien.auto.calimero.link.KNXLinkClosedException;
import tuwien.auto.calimero.log.LogManager;
import tuwien.auto.calimero.log.LogService;

/**
 * RF connection based on the RC1180 chip
 * 
 * @author Frederic Robra
 * 
 */
public class RC1180Connection {
	private static final boolean DEFAULT_AET = false;

	private final LogService logger;
	private SerialPort serialPort;
	private DataOutputStream os;
	private DataInputStream is;
	private final Receiver receiver;
	private volatile boolean open = false;
	private final Configure configure;
	private final Semaphore io = new Semaphore(1, true);
	private boolean addressExtensionType = DEFAULT_AET;

	private final EventListeners listeners = new EventListeners();

	private final Map<KNXAddress, Integer> linkLayerFrameNumbers = new LinkedHashMap<KNXAddress, Integer>();
	private final Map<KNXAddress, SNorDoA> addressContainer = new LinkedHashMap<KNXAddress, SNorDoA>();

	/**
	 * RF connection based on the RC1180 chip
	 * 
	 * @param portID
	 *            identifier of the serial device (e.g. /dev/ttyUSB0)
	 * @throws KNXException
	 */
	public RC1180Connection(String portID) throws KNXException {
		logger = LogManager.getManager().getLogService("RC1180 " + portID);

		try {
			io.acquireUninterruptibly();
			CommPortIdentifier commPortIdentifier = CommPortIdentifier.getPortIdentifier(portID);
			serialPort = (SerialPort) commPortIdentifier.open("knx", 2000);

			serialPort.setSerialPortParams(19200, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);

			os = new DataOutputStream(serialPort.getOutputStream());
			is = new DataInputStream(serialPort.getInputStream());

			int available = is.available();
			if (available > 0) {
				byte[] buffer = new byte[available];
				is.read(buffer);
			}
			open = true;
			logger.trace("connected to serial port " + serialPort.getName());
		} catch (Exception e) {
			logger.error(e.getMessage());
			throw new KNXException(e.getMessage(), e);
		} finally {
			io.release();
		}

		/* Configure CALAO USB-KNX-RF-C01 */
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
		}

		configure = new Configure(is, os, io, logger);

		configure.set();

		/* Create receiver, start listening */
		receiver = new Receiver();
		receiver.start();
	}

	public void putInAddressContainer(KNXAddress dst, SNorDoA address) {
		addressContainer.put(dst, address);
	}

	public Map<KNXAddress, SNorDoA> getAddressContainer() {
		return Collections.unmodifiableMap(addressContainer);
	}

	/**
	 * Same as sendRequest()<br>
	 * The function tries to find and set the source address and the serial number or domain address corresponding to
	 * the destination address.
	 * 
	 * @param dst
	 *            destination address
	 * @param hopCount
	 *            repetition counter
	 * @param nsdu
	 *            network layer service data unit
	 * @param wait
	 *            <code>true</code> to wait, <code>false</code> to immediately return
	 * @throws KNXLinkClosedException
	 */
	public void sendSpoofingRequest(IndividualAddress src, KNXAddress dst, int hopCount, byte[] nsdu, boolean wait)
			throws KNXLinkClosedException {
		if (addressContainer.containsKey(dst)) {
			logger.trace("send spoofing request");
			SNorDoA address = addressContainer.get(dst);
			addressExtensionType = address.isDomainAddress();
			if (addressExtensionType) {
				setDomainAddress(address.get());
			}
			else {
				setSerialNumber(address.get());
			}
		}
		sendRequest(src, dst, hopCount, nsdu, wait);
		addressExtensionType = DEFAULT_AET;
	}

	/**
	 * Send a message. If this is the first message to the destination, this method will send the message 8 times, so
	 * the frame number fits
	 * 
	 * @param src
	 *            source address
	 * @param dst
	 *            destination address
	 * @param hopCount
	 *            repetition counter
	 * @param nsdu
	 *            network layer service data unit
	 * @param wait
	 *            <code>true</code> to wait, <code>false</code> to immediately return
	 * @throws KNXLinkClosedException
	 */
	public void sendRequest(IndividualAddress src, KNXAddress dst, int hopCount, byte[] nsdu, boolean wait)
			throws KNXLinkClosedException {
		if (!linkLayerFrameNumbers.containsKey(dst)) { // if we don't know the frame number, take it over
			linkLayerFrameNumbers.put(dst, -1);
			for (int i = 0; i < 8; i++) {
				sendRequest(src, dst, hopCount, nsdu, true);
			}
			return;
		}

		int linkLayerFrameNumber = linkLayerFrameNumbers.get(dst);
		linkLayerFrameNumber = (linkLayerFrameNumber + 1) % 8;
		linkLayerFrameNumbers.put(dst, linkLayerFrameNumber);

		TransmittingFrame frame = new TransmittingFrame(src, dst, hopCount, linkLayerFrameNumber, addressExtensionType,
				nsdu);

		try {
			logger.trace("sending " + frame);
			io.acquireUninterruptibly();
			os.write(frame.getFrame());
			os.flush();

			if (wait) {
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {
				}
			}
		} catch (IOException e) {
			logger.error(e.getMessage());
			throw new KNXLinkClosedException(e.getMessage());
		} finally {
			io.release();
		}

	}

	/**
	 * 
	 * @return the status of the connection
	 */
	public boolean isOpen() {
		return open;
	}

	/**
	 * The connection is closed
	 */
	public void close() {
		if (open) {
			io.acquireUninterruptibly();
			receiver.stopReceiver();
			serialPort.close();
			open = false;
			io.release();
		}
	}

	/**
	 * Adds the specified event listener <code>l</code> to receive events from this connection.
	 * <p>
	 * If <code>l</code> was already added as listener, no action is performed.
	 * 
	 * @param l
	 *            the listener to add
	 */
	public void addConnectionListener(final KNXListener l) {
		listeners.add(l);
	}

	/**
	 * Removes the specified event listener <code>l</code>, so it does no longer receive events from this connection.
	 * <p>
	 * If <code>l</code> was not added in the first place, no action is performed.
	 * 
	 * @param l
	 *            the listener to remove
	 */
	public void removeConnectionListener(final KNXListener l) {
		listeners.remove(l);
	}

	/**
	 * Sets the serial number to the RC1180 chip
	 * 
	 * @param serialNumber
	 *            byte array containing the serial number
	 */
	public void setSerialNumber(final byte[] serialNumber) {
		try {
			configure.setSerialNumber(serialNumber);
		} catch (KNXException e) {
			logger.warn("failed to set serial number: " + DataUnitBuilder.toHex(serialNumber, ":"));
		}
	}

	/**
	 * Gets the serial number from the RC1180 chip
	 * 
	 * @return byte array containing the serial number
	 */
	public byte[] getSerialNumber() {
		byte[] serialNumber = null;
		try {
			serialNumber = configure.getSerialNumber();
		} catch (KNXException e) {
			logger.warn("failed to get serial number");
			serialNumber = new byte[6];
		}
		return serialNumber;
	}

	/**
	 * Sets the domain address to the RC1180 chip
	 * 
	 * @param domainAddress
	 *            byte array containing the domain address
	 */
	public void setDomainAddress(final byte[] domainAddress) {
		try {
			configure.setDomainAddress(domainAddress);
		} catch (KNXException e) {
			logger.warn("failed to set domain address: " + DataUnitBuilder.toHex(domainAddress, ":"));
		}
	}

	/**
	 * Gets the domain address from the RC1180 chip
	 * 
	 * @return byte array containing the domain address
	 */
	public byte[] getDomainAddress() {
		byte[] domainAddress = null;
		try {
			domainAddress = configure.getDomainAddress();
		} catch (KNXException e) {
			logger.warn("failed to get domain address");
			domainAddress = new byte[6];
		}
		return domainAddress;
	}

	private final class Receiver extends Thread {

		private static final byte START_BYTE = 0x68;
		private static final byte END_BYTE = 0x16;
		private static final int BUFFER_LENGTH = 255;

		private volatile boolean running;

		public Receiver() {
			super("RF receiver");
			setDaemon(true);
			running = false;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Thread#run()
		 */
		@Override
		public void run() {
			int available;
			int read = 0;
			int pos;
			byte[] buffer = new byte[BUFFER_LENGTH];
			running = true;
			while (running) {
				try {
					io.acquireUninterruptibly();
					available = is.available();
					if (available > 0) {
						is.read(buffer, read, available);
						read += available;

						/* Check if message reached end */
						while ((pos = containsEndByte(buffer)) > 0) {
							int nextPos = pos + 1;
							if (buffer[0] == START_BYTE) {
								byte[] message = new byte[pos - 1];

								System.arraycopy(buffer, 1, message, 0, message.length);

								try {
									/* parse buffer and fire event */
									logger.trace("received message: " + DataUnitBuilder.toHex(message, ":"));
									fireFrameReceived(new ReceivingFrame(message));
								} catch (KNXException e) {
									logger.warn("message " + DataUnitBuilder.toHex(message, ":")
											+ " could not be parsed: " + e.getMessage());
								}
							}
							else {
								logger.warn("start byte not received, skipping");
							}
							byte[] newBuffer = new byte[BUFFER_LENGTH];
							System.arraycopy(buffer, nextPos, newBuffer, 0, BUFFER_LENGTH - nextPos);
							buffer = newBuffer;
							read -= nextPos;
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
					logger.error(e.getMessage());
					running = false;
				} finally {
					io.release();
				}

				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
				}
			}
		}

		public synchronized void stopReceiver() {
			running = false;
		}

		private int containsEndByte(byte[] bytes) {
			for (int i = 0; i < bytes.length; i++) {
				if (bytes[i] == END_BYTE) {
					return i;
				}
			}
			return -1;
		}

		private void fireFrameReceived(ReceivingFrame frame) {
			KNXAddress dst = frame.getDstAddress();
			if (!linkLayerFrameNumbers.containsKey(dst)
					|| frame.getLinkLayerFrameNumber() == ((linkLayerFrameNumbers.get(dst) + 1) % 8)) {
				linkLayerFrameNumbers.put(frame.getDstAddress(), frame.getLinkLayerFrameNumber());
			}

			if (!addressContainer.containsKey(dst)) {
				addressContainer.put(dst, new SNorDoA(frame.getAET(), frame.getSNorDoA()));

			}

			FrameEvent event = new FrameEvent(this, frame.getCemilData());
			for (EventListener listener : listeners.listeners()) {
				try {
					((KNXListener) listener).frameReceived(event);
				} catch (final RuntimeException e) {
					removeConnectionListener((KNXListener) listener);
					logger.error("removed event listener", e);
				}
			}
		}
	}
}
