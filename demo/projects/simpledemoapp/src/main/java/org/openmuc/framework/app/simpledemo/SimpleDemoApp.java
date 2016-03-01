/*
 * This file is part of OpenMUC.
 * For more information visit http://www.openmuc.org
 *
 * You are free to use code of this sample file in any
 * way you like and without any restrictions.
 *
 */
package org.openmuc.framework.app.simpledemo;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

import org.openmuc.framework.data.ByteArrayValue;
import org.openmuc.framework.data.DoubleValue;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.data.StringValue;
import org.openmuc.framework.data.Value;
import org.openmuc.framework.dataaccess.Channel;
import org.openmuc.framework.dataaccess.DataAccessService;
import org.openmuc.framework.dataaccess.RecordListener;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SimpleDemoApp extends Thread implements RecordListener {

	private final static Logger logger = LoggerFactory.getLogger(SimpleDemoApp.class);

	private volatile boolean deactivatedSignal;

	// With this you can access to your measured and control data of your devices.
	DataAccessService dataAccessService;

	// Channel for accessing data of a channel.
	Channel channel1;
	Channel channel2;
	Channel channel3;
	Channel channel4;
	Channel channel5;
	Channel channel6;
	Channel channel7;

	// ChannelIDs, see conf/channel.xml
	public static final String CHANNEL_ID1 = "VoltageChannel";
	public static final String CHANNEL_ID2 = "CurrentChannel";
	public static final String CHANNEL_ID3 = "PowerChannel";
	public static final String CHANNEL_ID4 = "listeningChannel";
	public static final String CHANNEL_ID5 = "StringChannel";
	public static final String CHANNEL_ID6 = "ByteArrayChannel";
	public static final String CHANNEL_ID7 = "TimeSeriesStringChannel";

	int printCounter; // for slowing down the output of the console

	/**
	 * Every app needs one activate method. Is is called at begin. Here you can configure all you need at start of your
	 * app. The Activate method can block the start of your OpenMUC, f.e. if you use Thread.sleep().
	 * 
	 * @param context
	 */
	protected void activate(ComponentContext context) {
		logger.info("Activating Demo App");
		setName("OpenMUC Simple Demo App");
		start();
	}

	/**
	 * Every app needs one deactivate method. It handles the shutdown of your app. Here you can f.e. close streams.
	 * 
	 * @param context
	 */
	protected void deactivate(ComponentContext context) {
		logger.info("Deactivating Demo App");
		deactivatedSignal = true;
		interrupt();
		try {
			this.join();
		} catch (InterruptedException e) {
		}
	}

	/**
	 * To set the DataAccessService dataAccessService for your app.
	 * 
	 * @param dataAccessService
	 */
	protected void setDataAccessService(DataAccessService dataAccessService) {
		this.dataAccessService = dataAccessService;
	}

	/**
	 * To unset the DataAccessService dataAccessService for your app.
	 * 
	 * @param dataAccessService
	 */
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

		// Get channel with ChannelID.
		channel1 = dataAccessService.getChannel(CHANNEL_ID1);
		channel2 = dataAccessService.getChannel(CHANNEL_ID2);
		channel3 = dataAccessService.getChannel(CHANNEL_ID3);
		channel4 = dataAccessService.getChannel(CHANNEL_ID4);
		channel5 = dataAccessService.getChannel(CHANNEL_ID5);
		channel6 = dataAccessService.getChannel(CHANNEL_ID6);
		channel7 = dataAccessService.getChannel(CHANNEL_ID7);
		Value value = null;

		while (!deactivatedSignal && (channel1 == null || channel2 == null || channel3 == null || value == null)) {
			String errorMessage = "Channel " + CHANNEL_ID1 + " or " + CHANNEL_ID2 + " or " + CHANNEL_ID3
					+ " not found, will try again in 5 seconds.";
			logger.error(errorMessage);

			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				logger.info("DemoApp thread interrupted: will stop");
			}

			channel1 = dataAccessService.getChannel(CHANNEL_ID1);
			channel2 = dataAccessService.getChannel(CHANNEL_ID2);
			channel3 = dataAccessService.getChannel(CHANNEL_ID3);
			if (channel1 != null) {
				value = channel1.getLatestRecord().getValue();
			}
		}
		channel5 = dataAccessService.getChannel(CHANNEL_ID5);
		channel6 = dataAccessService.getChannel(CHANNEL_ID6);
		channel4.addListener(this);

		while (!deactivatedSignal) {

			double valueChannel1 = channel1.getLatestRecord().getValue().asDouble(); // voltage
			double valueChannel2 = channel2.getLatestRecord().getValue().asDouble(); // current
			double valueChannel3 = valueChannel1 * valueChannel2; // power
			String valueChannel5 = null;
			String valueChannel7 = null;
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

			channel3.setLatestRecord(new Record(new DoubleValue(valueChannel3), time));
			channel5.setLatestRecord(new Record(new StringValue(valueChannel5), time));

			// create a dummy TimeSeriesString to be written to channel7
			ArrayList<Double> data = new ArrayList<Double>();
			data.add(2.);
			data.add(3.);
			data.add(4.);
			data.add(3.5);
			data.add(2.2);
			data.add(1.5);
			data.add(0.5);
			data.add(-0.5);

			long time_3mins_ago = (time - 3 * 2000);
			long one_hot_minute = 2000;
			StringBuffer sb = new StringBuffer();
			for (Double double1 : data) {
				sb.append(String.valueOf(time_3mins_ago)).append(',').append(double1).append(';');
				time_3mins_ago += one_hot_minute;
			}
			valueChannel7 = sb.toString();
			channel7.setLatestRecord(new Record(new StringValue(valueChannel7), time));

			String state;
			if (valueChannel2 > 1.5) {
				state = "ON";
			}
			else {
				state = "OFF";
			}

			printCounter++;
			if (printCounter > 20) {
				logger.info(CHANNEL_ID1 + ": " + valueChannel1 + "  " + CHANNEL_ID2 + ": " + valueChannel2 + "  "
						+ CHANNEL_ID3 + ": " + valueChannel3 + "  state: " + state);
				printCounter = 0;
			}
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				logger.info("DemoApp thread interrupted");
			}
		}

		if (channel4 != null) {
			channel4.removeListener(this);
		}
	}

	/**
	 * This methods reacts on new records.
	 */
	@Override
	public void newRecord(Record record) {
		logger.info("Record listener got new record: " + record);
	}
}
