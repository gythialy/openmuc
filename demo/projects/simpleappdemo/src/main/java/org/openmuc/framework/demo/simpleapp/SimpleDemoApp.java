/*
 * Copyright 2011-14 Fraunhofer ISE
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

package org.openmuc.framework.demo.simpleapp;

import java.util.Calendar;
import java.util.GregorianCalendar;

import org.openmuc.framework.data.ByteArrayValue;
import org.openmuc.framework.data.DoubleValue;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.data.StringValue;
import org.openmuc.framework.dataaccess.Channel;
import org.openmuc.framework.dataaccess.DataAccessService;
import org.openmuc.framework.dataaccess.RecordListener;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SimpleDemoApp extends Thread implements RecordListener {

	private final static Logger logger = LoggerFactory.getLogger(SimpleDemoApp.class);

	private volatile boolean deactivatedSignal;

	DataAccessService dataAccessService;
	Channel channel1;
	Channel channel2;
	Channel channel3;
	Channel channel4;
	Channel channel5;
	Channel channel6;

	public static final String channelId1 = "VoltageChannel";
	public static final String channelId2 = "CurrentChannel";
	public static final String channelId3 = "PowerChannel";
	public static final String channelId4 = "listeningChannel";
	public static final String channelId5 = "StringChannel";
	public static final String channelId6 = "ByteArrayChannel";

	int printCounter; // for slowing down the output of the console

	protected void activate(ComponentContext context) {
		logger.info("Activating Demo App");
		setName("OpenMUC Simple Demo App");
		start();

	}

	protected void deactivate(ComponentContext context) {
		logger.info("Deactivating Demo App");
		deactivatedSignal = true;
		interrupt();
		try {
			this.join();
		} catch (InterruptedException e) {
		}
	}

	protected void setDataAccessService(DataAccessService dataAccessService) {
		this.dataAccessService = dataAccessService;
	}

	protected void unsetDataAccessService(DataAccessService dataAccessService) {
		this.dataAccessService = null;
	}

	@Override
	public void run() {

		logger.info("Demo App started running...");

		if (deactivatedSignal) {
			logger.info("DemoApp thread interrupted: will stop");
			return;
		}

		channel1 = dataAccessService.getChannel(channelId1);
		channel2 = dataAccessService.getChannel(channelId2);
		channel3 = dataAccessService.getChannel(channelId3);
		channel4 = dataAccessService.getChannel(channelId4);
		channel5 = dataAccessService.getChannel(channelId5);
		channel6 = dataAccessService.getChannel(channelId6);

		while (!deactivatedSignal && (channel1 == null || channel2 == null || channel3 == null)) {
			String errorMessage = "Channel " + channelId1 + " or " + channelId2 + " or " + channelId3
					+ " not found, will try again in 5 seconds.";
			logger.error(errorMessage);

			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				logger.info("DemoApp thread interrupted: will stop");
			}

			channel1 = dataAccessService.getChannel(channelId1);
			channel2 = dataAccessService.getChannel(channelId2);
			channel3 = dataAccessService.getChannel(channelId3);
		}
		channel5 = dataAccessService.getChannel(channelId5);
		channel6 = dataAccessService.getChannel(channelId6);
		channel4.addListener(this);
		// channel1.addListener(this);

		// WriteValueContainer writeContainer = channel1.getWriteContainer();
		// writeContainer.setValue(new DoubleValue(23.0));
		//
		// List<WriteValueContainer> writeContainerList = new ArrayList<WriteValueContainer>(1);
		// writeContainerList.add(writeContainer);

		while (!deactivatedSignal) {

			// List<Record> records = null;
			// try {
			// records = channel1.getLoggedRecords(System.currentTimeMillis() - 50000);
			// } catch (DataLoggerNotAvailableException e1) {
			// logger.error("Unable to get logged values: log source unavailable");
			// } catch (IOException e1) {
			// logger.error("Unable to get logged values: IOException: " + e1.getMessage());
			// }
			// if (records != null) {
			// for (Record record : records) {
			// logger.info("Got logged record: " + record);
			// }
			// }

			// Flag flag = channel1.write(new DoubleValue(33.0), 3000);
			// logger.info("Wrote to channel1, resulting flag was: " + flag);
			//
			// logger.info("Channel1 - latest record: " + channel1.getLatestRecord());
			// logger.info("Channel1 - channel state: " + channel1.getChannelState());
			// logger.info("Channel1 - device state: " + channel1.getDeviceState());
			//
			// logger.info("Channel2 - latest record: " + channel2.getLatestRecord());
			// logger.info("Channel2 - channel state: " + channel2.getChannelState());
			// logger.info("Channel2 - device state: " + channel2.getDeviceState());

			try {

				double valueChannel1 = channel1.getLatestRecord().getValue().asDouble(); // voltage
				double valueChannel2 = channel2.getLatestRecord().getValue().asDouble(); // current
				double valueChannel3 = valueChannel1 * valueChannel2; // power
				String valueChannel5 = null;
				Calendar cal = new GregorianCalendar();
				long time = cal.getTimeInMillis();
				if ((cal.get(Calendar.MINUTE) % 2) > 0) {
					valueChannel5 = "Teststring";
					byte[] valueChannel6 = { 0x00, 0x01, 0x09, 0x0A, 0x0F, 0x10, 0x11, 0x7F, -0x7F, -0x51, -0x10, -0x01 };
					channel6.setLatestRecord(new Record(new ByteArrayValue(valueChannel6), time));
				}
				else {
					valueChannel5 = "Test";
					byte[] valueChannel6 = { 0x00, 0x01, -0x10, -0x01 };
					channel6.setLatestRecord(new Record(new ByteArrayValue(valueChannel6), time));
				}
				// channel3.write(new DoubleValue(valueChannel3), 1000);
				channel3.setLatestRecord(new Record(new DoubleValue(valueChannel3), time));
				channel5.setLatestRecord(new Record(new StringValue(valueChannel5), time));
				String state;
				if (valueChannel2 > 1.5) {
					state = "ON";
				}
				else {
					state = "OFF";
				}

				printCounter++;
				if (printCounter > 20) {
					logger.debug(channelId1 + ": " + valueChannel1 + "  " + channelId2 + ": " + valueChannel2 + "  "
							+ channelId3 + ": " + valueChannel3 + "  state: " + state);
					printCounter = 0;
				}

			} catch (Exception e) {
				logger.error("something went wrong...");
			}

			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				logger.info("DemoApp thread interrupted");
			}
		}

		if (channel1 != null) {
			channel1.removeListener(this);
		}
	}

	@Override
	public void newRecord(Channel channel, Record record) {
		logger.info("Record listener got new record for channel: " + channel.getId() + " : " + record);
	}
}
