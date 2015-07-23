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
package tuwien.auto.calimero.link;

import java.util.Arrays;
import java.util.Map;

import tuwien.auto.calimero.CloseEvent;
import tuwien.auto.calimero.DataUnitBuilder;
import tuwien.auto.calimero.FrameEvent;
import tuwien.auto.calimero.KNXAddress;
import tuwien.auto.calimero.Priority;
import tuwien.auto.calimero.cemi.CEMILData;
import tuwien.auto.calimero.exception.KNXException;
import tuwien.auto.calimero.exception.KNXIllegalArgumentException;
import tuwien.auto.calimero.exception.KNXTimeoutException;
import tuwien.auto.calimero.link.medium.KNXMediumSettings;
import tuwien.auto.calimero.link.medium.RFSettings;
import tuwien.auto.calimero.log.LogManager;
import tuwien.auto.calimero.log.LogService;
import tuwien.auto.calimero.serial.rc1180.RC1180Connection;
import tuwien.auto.calimero.serial.rc1180.SNorDoA;

/**
 * Implementation of the KNX network link, for KNX RF, based on the RC1180 chip, using a {@link RC1180Connection}.
 * 
 * @author Frederic Robra
 * 
 */
public class KNXNetworkLinkRC1180 implements KNXNetworkLink {

	private static final class LinkNotifier extends EventNotifier {

		LinkNotifier(Object source, LogService logger) {
			super(source, logger);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see tuwien.auto.calimero.link.EventNotifier#frameReceived(tuwien.auto.calimero.FrameEvent)
		 */
		@Override
		public void frameReceived(FrameEvent e) {
			logger.trace(e.getFrame().toString());
			addEvent(new Indication(new FrameEvent(source, e.getFrame())));
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see tuwien.auto.calimero.link.EventNotifier#connectionClosed(tuwien.auto.calimero.CloseEvent)
		 */
		@Override
		public void connectionClosed(CloseEvent e) {
			((KNXNetworkLinkRC1180) source).close();
			super.connectionClosed(e);
			logger.info("link closed");
		}

	}

	private String name;
	private RC1180Connection connection;
	private LogService logger;
	private EventNotifier notifier;
	private RFSettings settings;
	private int hopCount = 6;
	private boolean spoofing;

	/**
	 * Creates a new network link for KNX RF. Opens a connection to a RC1180 chip, like the CALAO USB-KNX-RF-KNX1
	 * 
	 * @param portID
	 *            identifier of the serial device (e.g. /dev/ttyUSB0)
	 * @param settings
	 *            medium settings defining device and medium specifics needed for communication
	 * @throws KNXException
	 */
	public KNXNetworkLinkRC1180(String portID, KNXMediumSettings settings) throws KNXException {
		init(portID, settings);
	}

	/**
	 * Creates a new network link for KNX RF. Opens a connection to a RC1180 chip, like the CALAO USB-KNX-RF-KNX1
	 * <p>
	 * If spoofing is enabled, every message modifies the serial number or domain address in the non-volatile memory of
	 * the chip
	 * 
	 * @param portID
	 *            identifier of the serial device (e.g. /dev/ttyUSB0)
	 * @param settings
	 *            medium settings defining device and medium specifics needed for communication
	 * @param spoofing
	 *            enable for spoofing the serial number or domain address per send request
	 * @throws KNXException
	 */
	public KNXNetworkLinkRC1180(String portID, KNXMediumSettings settings, boolean spoofing) throws KNXException {
		this.spoofing = spoofing;
		init(portID, settings);
	}

	private void init(String portID, KNXMediumSettings settings) throws KNXException {
		name = "link " + portID;
		logger = LogManager.getManager().getLogService(name);
		connection = new RC1180Connection(portID);
		notifier = new LinkNotifier(this, logger);
		connection.addConnectionListener(notifier);
		setKNXMedium(settings);
	}

	public void addSendInformation(KNXAddress dst, SNorDoA address) {
		if (spoofing) {
			connection.putInAddressContainer(dst, address);
		}
	}

	public Map<KNXAddress, SNorDoA> getSendInformation() {
		return connection.getAddressContainer();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tuwien.auto.calimero.link.KNXNetworkLink#setKNXMedium(tuwien.auto.calimero.link.medium.KNXMediumSettings)
	 */
	@Override
	public void setKNXMedium(KNXMediumSettings settings) {
		if (settings == null) {
			throw new KNXIllegalArgumentException("medium settings are mandatory");
		}
		else if (settings instanceof RFSettings) {
			byte[] serialNumber = null;
			byte[] domainAddress = null;
			if (!Arrays.equals(((RFSettings) settings).getSerialNumber(), new byte[6])) {
				serialNumber = ((RFSettings) settings).getSerialNumber();
				connection.setSerialNumber(serialNumber);
			}
			else {
				serialNumber = connection.getSerialNumber();
			}
			if (!Arrays.equals(((RFSettings) settings).getDomainAddress(), new byte[6])) {
				domainAddress = ((RFSettings) settings).getDomainAddress();
				connection.setDomainAddress(domainAddress);
			}
			else {
				domainAddress = connection.getDomainAddress();
			}
			RFSettings acquiredSettings = new RFSettings(settings.getDeviceAddress(), domainAddress, serialNumber,
					((RFSettings) settings).isUnidirectional());

			logger.trace("serial number: " + DataUnitBuilder.toHex(serialNumber, ":") + "; domain address: "
					+ DataUnitBuilder.toHex(domainAddress, ":"));
			this.settings = acquiredSettings;
		}
		else {
			throw new KNXIllegalArgumentException("medium differs");
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tuwien.auto.calimero.link.KNXNetworkLink#getKNXMedium()
	 */
	@Override
	public KNXMediumSettings getKNXMedium() {
		return settings;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tuwien.auto.calimero.link.KNXNetworkLink#addLinkListener(tuwien.auto.calimero.link.NetworkLinkListener)
	 */
	@Override
	public void addLinkListener(NetworkLinkListener l) {
		notifier.addListener(l);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tuwien.auto.calimero.link.KNXNetworkLink#removeLinkListener(tuwien.auto.calimero.link.NetworkLinkListener)
	 */
	@Override
	public void removeLinkListener(NetworkLinkListener l) {
		notifier.removeListener(l);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tuwien.auto.calimero.link.KNXNetworkLink#setHopCount(int)
	 */
	@Override
	public void setHopCount(int count) {
		if (count < 0 || count > 7) {
			throw new KNXIllegalArgumentException("hop count out of range [0..7]");
		}
		hopCount = count;
		logger.info("hop count set to " + count);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tuwien.auto.calimero.link.KNXNetworkLink#getHopCount()
	 */
	@Override
	public int getHopCount() {
		return hopCount;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tuwien.auto.calimero.link.KNXNetworkLink#sendRequest(tuwien.auto.calimero.KNXAddress,
	 * tuwien.auto.calimero.Priority, byte[])
	 */
	@Override
	public void sendRequest(KNXAddress dst, Priority p, byte[] nsdu) throws KNXTimeoutException, KNXLinkClosedException {
		sendRequest(dst, hopCount, nsdu, false);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tuwien.auto.calimero.link.KNXNetworkLink#sendRequestWait(tuwien.auto.calimero.KNXAddress,
	 * tuwien.auto.calimero.Priority, byte[])
	 */
	@Override
	public void sendRequestWait(KNXAddress dst, Priority p, byte[] nsdu) throws KNXTimeoutException,
			KNXLinkClosedException {
		sendRequest(dst, hopCount, nsdu, true);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tuwien.auto.calimero.link.KNXNetworkLink#send(tuwien.auto.calimero.cemi.CEMILData, boolean)
	 */
	@Override
	public void send(CEMILData msg, boolean waitForCon) throws KNXTimeoutException, KNXLinkClosedException {
		sendRequest(msg.getDestination(), msg.getHopCount(), msg.getPayload(), waitForCon);
	}

	private void sendRequest(KNXAddress dst, int hopCount, byte[] nsdu, boolean waitForCon) throws KNXTimeoutException,
			KNXLinkClosedException {
		if (spoofing) {
			connection.sendSpoofingRequest(settings.getDeviceAddress(), dst, hopCount, nsdu, waitForCon);
		}
		else {
			connection.sendRequest(settings.getDeviceAddress(), dst, hopCount, nsdu, waitForCon);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tuwien.auto.calimero.link.KNXNetworkLink#getName()
	 */
	@Override
	public String getName() {
		return name;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tuwien.auto.calimero.link.KNXNetworkLink#isOpen()
	 */
	@Override
	public boolean isOpen() {
		return connection.isOpen();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tuwien.auto.calimero.link.KNXNetworkLink#close()
	 */
	@Override
	public void close() {
		connection.close();
	}

}
