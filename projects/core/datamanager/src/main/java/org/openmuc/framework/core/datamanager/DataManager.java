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

package org.openmuc.framework.core.datamanager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.locks.ReentrantLock;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.openmuc.framework.config.ArgumentSyntaxException;
import org.openmuc.framework.config.ChannelConfig;
import org.openmuc.framework.config.ChannelScanInfo;
import org.openmuc.framework.config.ConfigChangeListener;
import org.openmuc.framework.config.ConfigService;
import org.openmuc.framework.config.ConfigWriteException;
import org.openmuc.framework.config.DeviceConfig;
import org.openmuc.framework.config.DeviceScanInfo;
import org.openmuc.framework.config.DeviceScanListener;
import org.openmuc.framework.config.DriverConfig;
import org.openmuc.framework.config.DriverInfo;
import org.openmuc.framework.config.DriverNotAvailableException;
import org.openmuc.framework.config.ParseException;
import org.openmuc.framework.config.RootConfig;
import org.openmuc.framework.config.ScanException;
import org.openmuc.framework.config.ScanInterruptedException;
import org.openmuc.framework.config.ServerMapping;
import org.openmuc.framework.data.Flag;
import org.openmuc.framework.dataaccess.Channel;
import org.openmuc.framework.dataaccess.ChannelChangeListener;
import org.openmuc.framework.dataaccess.ChannelState;
import org.openmuc.framework.dataaccess.DataAccessService;
import org.openmuc.framework.dataaccess.DataLoggerNotAvailableException;
import org.openmuc.framework.dataaccess.DeviceState;
import org.openmuc.framework.dataaccess.LogicalDevice;
import org.openmuc.framework.dataaccess.LogicalDeviceChangeListener;
import org.openmuc.framework.dataaccess.ReadRecordContainer;
import org.openmuc.framework.dataaccess.WriteValueContainer;
import org.openmuc.framework.datalogger.spi.DataLoggerService;
import org.openmuc.framework.datalogger.spi.LogChannel;
import org.openmuc.framework.datalogger.spi.LogRecordContainer;
import org.openmuc.framework.driver.spi.ChannelRecordContainer;
import org.openmuc.framework.driver.spi.Connection;
import org.openmuc.framework.driver.spi.ConnectionException;
import org.openmuc.framework.driver.spi.DriverDeviceScanListener;
import org.openmuc.framework.driver.spi.DriverService;
import org.openmuc.framework.driver.spi.RecordsReceivedListener;
import org.openmuc.framework.server.spi.ServerMappingContainer;
import org.openmuc.framework.server.spi.ServerService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DataManager extends Thread implements DataAccessService, ConfigService, RecordsReceivedListener {

	private final static Logger logger = LoggerFactory.getLogger(DataManager.class);

	private volatile boolean stopFlag = false;

	private final HashMap<String, DriverService> newDrivers = new LinkedHashMap<String, DriverService>();
	private final HashMap<String, ServerService> serverServices = new HashMap<String, ServerService>();
	// does not need to be a list because RemovedService() for driver services
	// are never called in parallel:
	private volatile String driverToBeRemovedId = null;
	private volatile DataLoggerService dataLoggerToBeRemoved = null;

	final List<Device> connected = new LinkedList<Device>();
	final List<Device> disconnected = new LinkedList<Device>();
	final List<Device> connectionFailures = new LinkedList<Device>();
	final List<SamplingTask> samplingTaskFinished = new LinkedList<SamplingTask>();
	final List<WriteTask> newWriteTasks = new LinkedList<WriteTask>();
	final List<ReadTask> newReadTasks = new LinkedList<ReadTask>();
	final List<DeviceTask> tasksFinished = new LinkedList<DeviceTask>();
	private volatile RootConfigImpl newRootConfigWithoutDefaults = null;
	private final HashMap<String, DriverService> activeDrivers = new LinkedHashMap<String, DriverService>();

	private final List<Action> actions = new LinkedList<Action>();
	private final List<ConfigChangeListener> configChangeListeners = new LinkedList<ConfigChangeListener>();

	CountDownLatch dataLoggerRemovedSignal;
	private final List<DataLoggerService> newDataLoggers = new LinkedList<DataLoggerService>();
	private final Deque<DataLoggerService> activeDataLoggers = new LinkedBlockingDeque<DataLoggerService>();

	private final List<List<ChannelRecordContainer>> receivedRecordContainers = new LinkedList<List<ChannelRecordContainer>>();

	volatile int activeDeviceCountDown;

	private volatile RootConfigImpl rootConfig;
	private volatile RootConfigImpl rootConfigWithoutDefaults;

	private File configFile;

	ExecutorService executor = null;

	private volatile Boolean dataManagerActivated = false;

	CountDownLatch driverRemovedSignal;

	private CountDownLatch newConfigSignal;

	private final ReentrantLock configLock = new ReentrantLock();

	protected void activate(ComponentContext context) throws TransformerFactoryConfigurationError, IOException,
			ParserConfigurationException, TransformerException, ParseException {
		logger.info("Activating Data Manager");

		executor = Executors.newCachedThreadPool();

		String configFileName = System.getProperty("org.openmuc.framework.channelconfig");
		if (configFileName == null) {
			configFileName = "conf/channels.xml";
		}
		configFile = new File(configFileName);

		try {
			rootConfigWithoutDefaults = RootConfigImpl.createFromFile(configFile);
		} catch (FileNotFoundException e) {
			// create an empty configuration and store it in a file
			rootConfigWithoutDefaults = new RootConfigImpl();
			rootConfigWithoutDefaults.writeToFile(configFile);
			logger.info("No configuration file found. Created an empty config file at: " + configFile.getAbsolutePath());
		} catch (ParseException e) {
			throw new ParseException("Error parsing openmuc config file: " + e.getMessage());
		}

		rootConfig = new RootConfigImpl();

		applyConfiguration(rootConfigWithoutDefaults, System.currentTimeMillis());

		start();

		dataManagerActivated = true;
	}

	protected void deactivate(ComponentContext context) {
		logger.info("Deactivating Data Manager");

		stopFlag = true;
		interrupt();
		try {
			this.join();
			executor.shutdown();
		} catch (InterruptedException e) {
		}
		dataManagerActivated = false;
	}

	@Override
	public void run() {

		setName("OpenMUC Data Manager");
		handleInterruptEvent();

		while (!stopFlag) {

			if (interrupted()) {
				handleInterruptEvent();
				continue;
			}

			if (actions.size() == 0) {
				try {
					while (true) {
						Thread.sleep(Long.MAX_VALUE);
					}
				} catch (InterruptedException e) {
					handleInterruptEvent();
					continue;
				}
			}

			long sleepTime = 0;
			Action currentAction = actions.get(0);

			long currentTime = System.currentTimeMillis();

			if ((currentTime - currentAction.startTime) > 1000l) {
				actions.remove(0);
				logger.error("Action was scheduled for unix time "
						+ currentAction.startTime
						+ ". But current time is already "
						+ currentTime
						+ ". Will calculate new action time because the action has timed out. Has the system clock jumped?");
				if (currentAction.timeouts != null && currentAction.timeouts.size() > 0) {
					for (SamplingTask samplingTask : currentAction.timeouts) {
						samplingTask.timeout();
					}
				}
				if (currentAction.loggingCollections != null) {
					for (ChannelCollection loggingCollection : currentAction.loggingCollections) {
						addLoggingCollectionToActions(loggingCollection,
								loggingCollection.calculateNextActionTime(currentTime));
					}
				}
				if (currentAction.samplingCollections != null) {
					for (ChannelCollection samplingCollection : currentAction.samplingCollections) {
						addSamplingCollectionToActions(samplingCollection,
								samplingCollection.calculateNextActionTime(currentTime));
					}
				}
				if (currentAction.connectionRetryDevices != null) {
					for (Device device : currentAction.connectionRetryDevices) {
						addReconnectDeviceToActions(device, currentTime + device.deviceConfig.getConnectRetryInterval());
					}
				}
				continue;
			}

			sleepTime = currentAction.startTime - currentTime;
			if (sleepTime > 0) {
				try {
					Thread.sleep(sleepTime);
				} catch (InterruptedException e1) {
					handleInterruptEvent();
					continue;
				}
			}
			actions.remove(0);

			if (currentAction.timeouts != null && currentAction.timeouts.size() > 0) {
				for (SamplingTask samplingTask : currentAction.timeouts) {
					samplingTask.timeout();
				}
			}

			if (currentAction.loggingCollections != null && currentAction.loggingCollections.size() > 0) {

				List<LogRecordContainer> logContainers = new LinkedList<LogRecordContainer>();
				List<ChannelImpl> toRemove = new LinkedList<ChannelImpl>();

				for (ChannelCollection loggingCollection : currentAction.loggingCollections) {
					toRemove.clear();
					for (ChannelImpl channel : loggingCollection.channels) {
						if (channel.config.state == ChannelState.DELETED) {
							toRemove.add(channel);
						}
						else if (!channel.config.isDisabled()) {
							logContainers.add(new LogRecordContainerImpl(channel.config.id, channel.latestRecord));
						}
					}

					for (ChannelImpl channel : toRemove) {
						loggingCollection.channels.remove(channel);
					}

					if (loggingCollection.channels.size() > 0) {
						addLoggingCollectionToActions(loggingCollection, currentAction.startTime
								+ loggingCollection.interval);
					}
				}
				for (DataLoggerService dataLogger : activeDataLoggers) {
					dataLogger.log(logContainers, currentAction.startTime);
				}
			}

			if (currentAction.connectionRetryDevices != null && currentAction.connectionRetryDevices.size() > 0) {
				for (Device device : currentAction.connectionRetryDevices) {
					device.connectRetrySignal();
				}
			}

			if (currentAction.samplingCollections != null && currentAction.samplingCollections.size() > 0) {

				for (ChannelCollection samplingCollection : currentAction.samplingCollections) {
					List<ChannelRecordContainerImpl> selectedChannels = new ArrayList<ChannelRecordContainerImpl>(
							samplingCollection.channels.size());
					for (ChannelImpl channel : samplingCollection.channels) {
						selectedChannels.add(channel.createChannelRecordContainer());
					}
					SamplingTask samplingTask = new SamplingTask(this, samplingCollection.device, selectedChannels,
							samplingCollection.samplingGroup);

					int timeout = samplingCollection.device.deviceConfig.samplingTimeout;

					if (samplingCollection.device.addSamplingTask(samplingTask, samplingCollection.interval)
							&& timeout > 0) {
						addSamplingWorkerTimeoutToActions(samplingTask, currentAction.startTime
								+ samplingCollection.device.deviceConfig.samplingTimeout);
					}

					addSamplingCollectionToActions(samplingCollection, currentAction.startTime
							+ samplingCollection.interval);
				}

			}
		}
	}

	private void addSamplingCollectionToActions(ChannelCollection channelCollection, long startTimestamp) {

		Action fittingAction = null;

		ListIterator<Action> actionIterator = actions.listIterator();
		while (actionIterator.hasNext()) {
			Action currentAction = actionIterator.next();
			if (currentAction.startTime == startTimestamp) {
				fittingAction = currentAction;
				if (fittingAction.samplingCollections == null) {
					fittingAction.samplingCollections = new LinkedList<ChannelCollection>();
				}
				break;
			}
			else if (currentAction.startTime > startTimestamp) {
				fittingAction = new Action(startTimestamp);
				fittingAction.samplingCollections = new LinkedList<ChannelCollection>();
				actionIterator.previous();
				actionIterator.add(fittingAction);
				break;
			}
		}

		if (fittingAction == null) {
			fittingAction = new Action(startTimestamp);
			fittingAction.samplingCollections = new LinkedList<ChannelCollection>();
			actions.add(fittingAction);
		}

		fittingAction.samplingCollections.add(channelCollection);
		channelCollection.action = fittingAction;

	}

	private void addLoggingCollectionToActions(ChannelCollection channelCollection, long startTimestamp) {

		Action fittingAction = null;

		ListIterator<Action> actionIterator = actions.listIterator();
		while (actionIterator.hasNext()) {
			Action currentAction = actionIterator.next();
			if (currentAction.startTime == startTimestamp) {
				fittingAction = currentAction;
				if (fittingAction.loggingCollections == null) {
					fittingAction.loggingCollections = new LinkedList<ChannelCollection>();
				}
				break;
			}
			else if (currentAction.startTime > startTimestamp) {
				fittingAction = new Action(startTimestamp);
				fittingAction.loggingCollections = new LinkedList<ChannelCollection>();
				actionIterator.previous();
				actionIterator.add(fittingAction);
				break;
			}
		}

		if (fittingAction == null) {
			fittingAction = new Action(startTimestamp);
			fittingAction.loggingCollections = new LinkedList<ChannelCollection>();
			actions.add(fittingAction);
		}

		fittingAction.loggingCollections.add(channelCollection);
		channelCollection.action = fittingAction;
	}

	void addReconnectDeviceToActions(Device device, long startTimestamp) {
		Action fittingAction = null;

		ListIterator<Action> actionIterator = actions.listIterator();
		while (actionIterator.hasNext()) {
			Action currentAction = actionIterator.next();
			if (currentAction.startTime == startTimestamp) {
				fittingAction = currentAction;
				if (fittingAction.connectionRetryDevices == null) {
					fittingAction.connectionRetryDevices = new LinkedList<Device>();
				}
				break;
			}
			else if (currentAction.startTime > startTimestamp) {
				fittingAction = new Action(startTimestamp);
				fittingAction.connectionRetryDevices = new LinkedList<Device>();
				actionIterator.previous();
				actionIterator.add(fittingAction);
				break;
			}
		}

		if (fittingAction == null) {
			fittingAction = new Action(startTimestamp);
			fittingAction.connectionRetryDevices = new LinkedList<Device>();
			actions.add(fittingAction);
		}

		fittingAction.connectionRetryDevices.add(device);
	}

	private void addSamplingWorkerTimeoutToActions(SamplingTask readWorker, long timeout) {

		Action fittingAction = null;

		ListIterator<Action> actionIterator = actions.listIterator();
		while (actionIterator.hasNext()) {
			Action currentAction = actionIterator.next();
			if (currentAction.startTime == timeout) {
				fittingAction = currentAction;
				if (fittingAction.timeouts == null) {
					fittingAction.timeouts = new LinkedList<SamplingTask>();
				}
				break;
			}
			else if (currentAction.startTime > timeout) {
				fittingAction = new Action(timeout);
				fittingAction.timeouts = new LinkedList<SamplingTask>();
				actionIterator.previous();
				actionIterator.add(fittingAction);
				break;
			}
		}

		if (fittingAction == null) {
			fittingAction = new Action(timeout);
			fittingAction.timeouts = new LinkedList<SamplingTask>();
			actions.add(fittingAction);
		}

		fittingAction.timeouts.add(readWorker);
	}

	private void handleInterruptEvent() {

		if (stopFlag) {
			prepareStop();
			return;
		}

		long currentTime = 0;

		if (newRootConfigWithoutDefaults != null) {
			currentTime = System.currentTimeMillis();
			applyConfiguration(newRootConfigWithoutDefaults, currentTime);
			newRootConfigWithoutDefaults = null;
			newConfigSignal.countDown();
		}

		synchronized (receivedRecordContainers) {
			if (receivedRecordContainers.size() != 0) {
				for (List<ChannelRecordContainer> recordContainers : receivedRecordContainers) {
					for (ChannelRecordContainer container : recordContainers) {
						ChannelRecordContainerImpl containerImpl = (ChannelRecordContainerImpl) container;
						if (containerImpl.channel.config.state == ChannelState.LISTENING) {
							containerImpl.channel.setNewRecord(containerImpl.record);
						}
					}
				}
			}
			receivedRecordContainers.clear();
		}

		synchronized (samplingTaskFinished) {
			if (samplingTaskFinished.size() != 0) {
				for (SamplingTask samplingTask : samplingTaskFinished) {
					samplingTask.storeValues();
					samplingTask.device.taskFinished();
				}
				samplingTaskFinished.clear();
			}
		}

		synchronized (tasksFinished) {
			if (tasksFinished.size() != 0) {
				for (DeviceTask deviceTask : tasksFinished) {
					deviceTask.device.taskFinished();
				}
				tasksFinished.clear();
			}
		}

		synchronized (newDrivers) {
			if (newDrivers.size() != 0) {
				// needed to synchronize with getRunningDrivers
				synchronized (activeDrivers) {
					activeDrivers.putAll(newDrivers);
				}
				for (Entry<String, DriverService> newDriverEntry : newDrivers.entrySet()) {
					logger.info("Driver registered: " + newDriverEntry.getKey());
					DriverConfigImpl driverConfig = rootConfig.driverConfigsById.get(newDriverEntry.getKey());
					if (driverConfig != null) {
						driverConfig.activeDriver = newDriverEntry.getValue();
						for (DeviceConfigImpl deviceConfig : driverConfig.deviceConfigsById.values()) {
							deviceConfig.device.driverRegisteredSignal();
						}
					}
				}
				newDrivers.clear();
			}
		}

		synchronized (newDataLoggers) {
			if (newDataLoggers.size() != 0) {
				activeDataLoggers.addAll(newDataLoggers);
				for (DataLoggerService dataLogger : newDataLoggers) {
					logger.info("Data logger registered: " + dataLogger.getId());
					dataLogger.setChannelsToLog(rootConfig.logChannels);
				}
				newDataLoggers.clear();
			}
		}

		if (driverToBeRemovedId != null) {

			DriverService removedDriverService;
			synchronized (activeDrivers) {
				removedDriverService = activeDrivers.remove(driverToBeRemovedId);
			}

			if (removedDriverService == null) {
				// drivers was removed before it was added to activeDrivers
				newDrivers.remove(driverToBeRemovedId);
				driverRemovedSignal.countDown();
			}
			else {
				DriverConfigImpl driverConfig = rootConfig.driverConfigsById.get(driverToBeRemovedId);

				if (driverConfig != null) {
					activeDeviceCountDown = driverConfig.deviceConfigsById.size();
					if (activeDeviceCountDown > 0) {

						// all devices have to be given a chance to finish their current task and disconnect:
						for (DeviceConfigImpl deviceConfig : driverConfig.deviceConfigsById.values()) {
							deviceConfig.device.driverDeregisteredSignal();
						}
						synchronized (driverRemovedSignal) {
							if (activeDeviceCountDown == 0) {
								driverRemovedSignal.countDown();
							}
						}
					}
					else {
						driverRemovedSignal.countDown();
					}
				}
				else {
					driverRemovedSignal.countDown();
				}
			}
			driverToBeRemovedId = null;
		}

		if (dataLoggerToBeRemoved != null) {
			if (activeDataLoggers.remove(dataLoggerToBeRemoved) == false) {
				newDataLoggers.remove(dataLoggerToBeRemoved);
			}
			dataLoggerToBeRemoved = null;
			dataLoggerRemovedSignal.countDown();
		}

		synchronized (connectionFailures) {
			if (connectionFailures.size() != 0) {
				if (currentTime == 0) {
					currentTime = System.currentTimeMillis();
				}
				for (Device connectionFailureDevice : connectionFailures) {
					connectionFailureDevice.connectFailureSignal(currentTime);
				}
				connectionFailures.clear();
			}
		}

		synchronized (connected) {
			if (connected.size() != 0) {
				if (currentTime == 0) {
					currentTime = System.currentTimeMillis();
				}
				for (Device connectedDevice : connected) {
					connectedDevice.connectedSignal(currentTime);
				}
				connected.clear();
			}
		}

		synchronized (newWriteTasks) {
			if (newWriteTasks.size() != 0) {
				for (WriteTask newWriteTask : newWriteTasks) {
					newWriteTask.device.addWriteTask(newWriteTask);
				}
				newWriteTasks.clear();
			}
		}

		synchronized (newReadTasks) {
			if (newReadTasks.size() != 0) {
				for (ReadTask newReadTask : newReadTasks) {
					newReadTask.device.addReadTask(newReadTask);
				}
				newReadTasks.clear();
			}
		}

		synchronized (disconnected) {
			if (disconnected.size() != 0) {
				for (Device connectedDevice : disconnected) {
					connectedDevice.disconnectedSignal();
				}
				disconnected.clear();
			}
		}

	}

	private void applyConfiguration(RootConfigImpl configWithoutDefaults, long currentTime) {

		RootConfigImpl newRootConfig = configWithoutDefaults.cloneWithDefaults();

		List<LogChannel> logChannels = new LinkedList<LogChannel>();

		for (DriverConfigImpl oldDriverConfig : rootConfig.driverConfigsById.values()) {
			DriverConfigImpl newDriverConfig = newRootConfig.driverConfigsById.get(oldDriverConfig.id);
			if (newDriverConfig != null) {
				newDriverConfig.activeDriver = oldDriverConfig.activeDriver;
			}
			for (DeviceConfigImpl oldDeviceConfig : oldDriverConfig.deviceConfigsById.values()) {
				DeviceConfigImpl newDeviceConfig = null;
				if (newDriverConfig != null) {
					newDeviceConfig = newDriverConfig.deviceConfigsById.get(oldDeviceConfig.id);
				}
				if (newDeviceConfig == null) {
					// Device was deleted in new config
					oldDeviceConfig.device.deleteSignal();
				}
				else {
					// Device exists in new and old config
					oldDeviceConfig.device.configChangedSignal(newDeviceConfig, currentTime, logChannels);
				}
			}
		}

		for (DriverConfigImpl newDriverConfig : newRootConfig.driverConfigsById.values()) {
			DriverConfigImpl oldDriverConfig = rootConfig.driverConfigsById.get(newDriverConfig.id);
			if (oldDriverConfig == null) {
				newDriverConfig.activeDriver = activeDrivers.get(newDriverConfig.id);
			}
			for (DeviceConfigImpl newDeviceConfig : newDriverConfig.deviceConfigsById.values()) {

				DeviceConfigImpl oldDeviceConfig = null;
				if (oldDriverConfig != null) {
					oldDeviceConfig = oldDriverConfig.deviceConfigsById.get(newDeviceConfig.id);
				}

				if (oldDeviceConfig == null) {
					// Device is new
					newDeviceConfig.device = new Device(this, newDeviceConfig, currentTime, logChannels);
					if (newDeviceConfig.device.state == DeviceState.CONNECTING) {
						newDeviceConfig.device.connectRetrySignal();
					}
				}
			}
		}

		for (ChannelConfigImpl oldChannelConfig : rootConfig.channelConfigsById.values()) {
			ChannelConfigImpl newChannelConfig = newRootConfig.channelConfigsById.get(oldChannelConfig.getId());
			if (newChannelConfig == null) {
				// oldChannelConfig does not exist in the new configuration
				if (oldChannelConfig.state == ChannelState.SAMPLING) {
					removeFromSamplingCollections(oldChannelConfig.channel);
				}
				oldChannelConfig.state = ChannelState.DELETED;
				oldChannelConfig.channel.setFlag(Flag.CHANNEL_DELETED);
				// note: disabling SampleTasks and such has to be done at the
				// Device level
			}

		}

		for (DataLoggerService dataLogger : activeDataLoggers) {
			dataLogger.setChannelsToLog(logChannels);
		}

		newRootConfig.logChannels = logChannels;

		synchronized (configChangeListeners) {

			rootConfig = newRootConfig;
			rootConfigWithoutDefaults = configWithoutDefaults;

			for (ConfigChangeListener configChangeListener : configChangeListeners) {
				if (configChangeListener != null) {
					// TODO this should be done in a separate thread
					configChangeListener.configurationChanged();
				}
			}
		}

		notifyServers();
	}

	void addToSamplingCollections(ChannelImpl channel, Long time) {

		ChannelCollection fittingSamplingCollection = null;
		for (Action action : actions) {
			if (action.samplingCollections != null) {
				for (ChannelCollection samplingCollection : action.samplingCollections) {
					if (samplingCollection.interval == channel.config.samplingInterval
							&& samplingCollection.timeOffset == channel.config.samplingTimeOffset
							&& samplingCollection.samplingGroup.equals(channel.config.samplingGroup)
							&& samplingCollection.device == channel.config.deviceParent.device) {
						fittingSamplingCollection = samplingCollection;
						break;
					}
				}
			}
		}

		if (fittingSamplingCollection == null) {
			fittingSamplingCollection = new ChannelCollection(channel.config.samplingInterval,
					channel.config.samplingTimeOffset, channel.config.samplingGroup, channel.config.deviceParent.device);
			addSamplingCollectionToActions(fittingSamplingCollection,
					fittingSamplingCollection.calculateNextActionTime(time));
		}

		if (channel.samplingCollection != null) {
			if (channel.samplingCollection != fittingSamplingCollection) {
				removeFromSamplingCollections(channel);
			}
			else {
				return;
			}
		}
		fittingSamplingCollection.channels.add(channel);
		channel.samplingCollection = fittingSamplingCollection;
	}

	void addToLoggingCollections(ChannelImpl channel, Long time) {
		ChannelCollection fittingLoggingCollection = null;
		for (Action action : actions) {
			if (action.loggingCollections != null) {
				for (ChannelCollection loggingCollection : action.loggingCollections) {
					if (loggingCollection.interval == channel.config.loggingInterval
							&& loggingCollection.timeOffset == channel.config.loggingTimeOffset) {
						fittingLoggingCollection = loggingCollection;
						break;
					}
				}
			}
		}
		if (fittingLoggingCollection == null) {
			fittingLoggingCollection = new ChannelCollection(channel.config.loggingInterval,
					channel.config.loggingTimeOffset, null, null);
			addLoggingCollectionToActions(fittingLoggingCollection,
					fittingLoggingCollection.calculateNextActionTime(time));
		}

		if (channel.loggingCollection != null) {
			if (channel.loggingCollection != fittingLoggingCollection) {
				removeFromLoggingCollections(channel);
			}
			else {
				return;
			}
		}

		fittingLoggingCollection.channels.add(channel);
		channel.loggingCollection = fittingLoggingCollection;
	}

	void removeFromLoggingCollections(ChannelImpl channel) {
		channel.loggingCollection.channels.remove(channel);
		if (channel.loggingCollection.channels.size() == 0) {
			channel.loggingCollection.action.loggingCollections.remove(channel.loggingCollection);
		}
		channel.loggingCollection = null;
	}

	void removeFromSamplingCollections(ChannelImpl channel) {
		channel.samplingCollection.channels.remove(channel);
		if (channel.samplingCollection.channels.size() == 0) {
			channel.samplingCollection.action.samplingCollections.remove(channel.samplingCollection);
		}
		channel.samplingCollection = null;
	}

	void removeFromConnectionRetry(Device device) {
		for (Action action : actions) {
			if (action.connectionRetryDevices != null && action.connectionRetryDevices.remove(device)) {
				break;
			}
		}
	}

	protected void setDriverService(DriverService driver) {

		String driverId = driver.getInfo().getId();

		synchronized (newDrivers) {
			if (activeDrivers.get(driverId) != null || newDrivers.get(driverId) != null) {
				logger.error("Unable to register driver: a driver with the ID " + driverId + " is already registered.");
				return;
			}
			newDrivers.put(driverId, driver);
			interrupt();
		}
	}

	/**
	 * Registeres a new ServerService.
	 * 
	 * @param serverService
	 */
	protected void setServerService(ServerService serverService) {
		String serverId = serverService.getId();
		serverServices.put(serverId, serverService);

		if (dataManagerActivated) {
			notifyServer(serverService);
		}

		logger.info("Registered Server: " + serverId);
	}

	/**
	 * Removes a registered ServerService.
	 */
	protected void unsetServerService(ServerService serverService) {
		serverServices.remove(serverService.getId());
	}

	/**
	 * Updates all ServerServices with mapped channels.
	 */
	protected void notifyServers() {
		if (serverServices != null) {
			for (ServerService serverService : serverServices.values()) {
				notifyServer(serverService);
			}
		}
	}

	/**
	 * Updates a specified ServerService with mapped channels.
	 * 
	 * @param serverService
	 */
	protected void notifyServer(ServerService serverService) {
		List<ServerMappingContainer> relatedServerMappings = new ArrayList<ServerMappingContainer>();

		for (ChannelConfig config : rootConfig.channelConfigsById.values()) {
			for (ServerMapping serverMapping : config.getServerMappings()) {
				if (serverMapping.getId().equals(serverService.getId())) {
					relatedServerMappings
							.add(new ServerMappingContainer(this.getChannel(config.getId()), serverMapping));
				}
			}
		}
		serverService.serverMappings(relatedServerMappings);
	}

	protected void unsetDriverService(DriverService driver) {

		String driverId = driver.getInfo().getId();
		logger.info("Deregistering driver: " + driverId);

		// note: no synchronization needed here because this function and the
		// deactivate function are always called sequentially:
		if (dataManagerActivated) {
			driverToBeRemovedId = driverId;
			driverRemovedSignal = new CountDownLatch(1);
			interrupt();
			try {
				driverRemovedSignal.await();
			} catch (InterruptedException e) {
			}
		}
		else {
			if (activeDrivers.remove(driverId) == null) {
				newDrivers.remove(driverId);
			}
		}
		logger.info("Driver deregistered: " + driverId);
	}

	protected void setDataLoggerService(DataLoggerService dataLogger) {
		synchronized (newDataLoggers) {
			newDataLoggers.add(dataLogger);
			interrupt();
		}
	}

	protected void unsetDataLoggerService(DataLoggerService dataLogger) {

		String dataLoggerId = dataLogger.getId();
		logger.info("Deregistering data logger: " + dataLoggerId);

		if (dataManagerActivated) {
			dataLoggerRemovedSignal = new CountDownLatch(1);
			dataLoggerToBeRemoved = dataLogger;
			interrupt();
			try {
				dataLoggerRemovedSignal.await();
			} catch (InterruptedException e) {
			}
		}
		else {
			if (activeDataLoggers.remove(dataLogger) == false) {
				newDataLoggers.remove(dataLogger);
			}
		}

		logger.info("Data logger deregistered: " + dataLoggerId);

	}

	private void prepareStop() {
		// TODO tell all drivers to stop listening
		// Do I have to wait for all threads (such as SamplingTasks) to finish?
		executor.shutdown();
	}

	@Override
	public Channel getChannel(String id) {
		ChannelConfigImpl channelConfig = rootConfig.channelConfigsById.get(id);
		if (channelConfig == null) {
			return null;
		}
		return channelConfig.channel;
	}

	@Override
	public Channel getChannel(String id, ChannelChangeListener channelChangeListener) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<LogicalDevice> getLogicalDevices(String type) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<LogicalDevice> getLogicalDevices(String type, LogicalDeviceChangeListener logicalDeviceChangeListener) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void newRecords(List<ChannelRecordContainer> recordContainers) {
		List<ChannelRecordContainer> recordContainersCopy = new ArrayList<ChannelRecordContainer>(
				recordContainers.size());
		for (ChannelRecordContainer container : recordContainers) {
			recordContainersCopy.add(container.copy());
		}
		synchronized (receivedRecordContainers) {
			receivedRecordContainers.add(recordContainersCopy);
		}

		interrupt();

	}

	@Override
	public void connectionInterrupted(String driverId, Connection connection) {
		// TODO synchronize here
		DriverConfig driverConfig = rootConfig.getDriver(driverId);
		if (driverConfig == null) {
			return;
		}
		for (DeviceConfig deviceConfig : driverConfig.getDevices()) {
			if (((DeviceConfigImpl) deviceConfig).device.connection == connection) {
				Device device = ((DeviceConfigImpl) deviceConfig).device;
				logger.info("Connection to device " + device.deviceConfig.id + " was interrupted.");
				device.disconnectedSignal();
				return;
			}
		}
	}

	@Override
	public void lock() {
		configLock.lock();
	}

	@Override
	public boolean tryLock() {
		return configLock.tryLock();
	}

	@Override
	public void unlock() {
		configLock.unlock();
	}

	@Override
	public RootConfig getConfig() {
		return rootConfigWithoutDefaults.clone();
	}

	@Override
	public RootConfig getConfig(ConfigChangeListener listener) {
		synchronized (configChangeListeners) {
			for (ConfigChangeListener weakChangeListener : configChangeListeners) {
				configChangeListeners.remove(weakChangeListener);
			}
			configChangeListeners.add(listener);
			return getConfig();
		}
	}

	@Override
	public void stopListeningForConfigChange(ConfigChangeListener listener) {
		synchronized (configChangeListeners) {
			for (ConfigChangeListener weakChangeListener : configChangeListeners) {
				if (weakChangeListener == listener) {
					configChangeListeners.remove(weakChangeListener);
				}
			}
		}
	}

	@Override
	public void setConfig(RootConfig config) {
		configLock.lock();
		try {
			RootConfigImpl newConfigCopy = ((RootConfigImpl) config).clone();
			setNewConfig(newConfigCopy);
		} finally {
			configLock.unlock();
		}

	}

	@Override
	public void reloadConfigFromFile() throws FileNotFoundException, ParseException {
		configLock.lock();
		try {
			RootConfigImpl newConfigCopy = RootConfigImpl.createFromFile(configFile);
			setNewConfig(newConfigCopy);
		} finally {
			configLock.unlock();
		}

	}

	private void setNewConfig(RootConfigImpl newConfigCopy) {
		synchronized (this) {
			newConfigSignal = new CountDownLatch(1);
			newRootConfigWithoutDefaults = newConfigCopy;
			interrupt();
		}
		while (true) {
			try {
				newConfigSignal.await();
				break;
			} catch (InterruptedException e) {
			}
		}

	}

	@Override
	public RootConfig getEmptyConfig() {
		return new RootConfigImpl();
	}

	@Override
	public void writeConfigToFile() throws ConfigWriteException {
		try {
			rootConfigWithoutDefaults.writeToFile(configFile);
		} catch (Exception e) {
			throw new ConfigWriteException(e);
		}
	}

	class BlockingScanListener implements DriverDeviceScanListener {
		List<DeviceScanInfo> scanInfos = new ArrayList<DeviceScanInfo>();

		@Override
		public void scanProgressUpdate(int progress) {
		}

		@Override
		public void deviceFound(DeviceScanInfo scanInfo) {
			if (!scanInfos.contains(scanInfo)) {
				scanInfos.add(scanInfo);
			}
		}

	}

	@Override
	public List<DeviceScanInfo> scanForDevices(String driverId, String settings) throws DriverNotAvailableException,
			UnsupportedOperationException, ArgumentSyntaxException, ScanException, ScanInterruptedException {

		DriverService driver = activeDrivers.get(driverId);
		if (driver == null) {
			throw new DriverNotAvailableException();
		}

		BlockingScanListener blockingScanListener = new BlockingScanListener();

		driver.scanForDevices(settings, blockingScanListener);

		return blockingScanListener.scanInfos;

	}

	@Override
	public void scanForDevices(String driverId, String settings, DeviceScanListener scanListener)
			throws DriverNotAvailableException {
		DriverService driver = activeDrivers.get(driverId);
		if (driver == null) {
			throw new DriverNotAvailableException();
		}
		executor.execute(new ScanForDevicesTask(driver, settings, scanListener));
	}

	@Override
	public void interruptDeviceScan(String driverId) throws DriverNotAvailableException, UnsupportedOperationException {
		DriverService driver = activeDrivers.get(driverId);
		if (driver == null) {
			throw new DriverNotAvailableException();
		}
		driver.interruptDeviceScan();
	}

	@Override
	public List<ChannelScanInfo> scanForChannels(String deviceId, String settings) throws DriverNotAvailableException,
			UnsupportedOperationException, ArgumentSyntaxException, ScanException {
		// TODO this function is probably not thread safe

		DeviceConfigImpl config = (DeviceConfigImpl) rootConfig.getDevice(deviceId);
		if (config == null) {
			throw new ScanException("No device with ID \"" + deviceId + "\" found.");
		}
		DriverService activeDriver = activeDrivers.get(config.driverParent.id);
		if (activeDriver == null) {
			throw new DriverNotAvailableException();
		}
		try {
			return config.device.connection.scanForChannels(settings);
		} catch (ConnectionException e) {
			config.device.disconnectedSignal();
			throw new ScanException(e.getMessage(), e);
		}
	}

	@Override
	public List<String> getIdsOfRunningDrivers() {
		List<String> availableDrivers;
		synchronized (activeDrivers) {
			availableDrivers = new ArrayList<String>(activeDrivers.size());
			for (String activeDriverName : activeDrivers.keySet()) {
				availableDrivers.add(activeDriverName);
			}
		}
		return availableDrivers;
	}

	@Override
	public void write(List<WriteValueContainer> values) {
		HashMap<Device, List<WriteValueContainerImpl>> containersByDevice = new LinkedHashMap<Device, List<WriteValueContainerImpl>>();

		for (WriteValueContainer value : values) {
			if (value.getValue() == null) {
				((WriteValueContainerImpl) value).setFlag(Flag.CANNOT_WRITE_NULL_VALUE);
				continue;
			}
			WriteValueContainerImpl valueContainerImpl = (WriteValueContainerImpl) value;
			Device device = valueContainerImpl.channel.config.deviceParent.device;
			List<WriteValueContainerImpl> writeValueContainers = containersByDevice.get(device);
			if (writeValueContainers == null) {
				writeValueContainers = new LinkedList<WriteValueContainerImpl>();
				containersByDevice.put(device, writeValueContainers);
			}
			writeValueContainers.add(valueContainerImpl);
		}
		CountDownLatch writeTasksFinishedSignal = new CountDownLatch(containersByDevice.size());

		synchronized (newWriteTasks) {
			for (Entry<Device, List<WriteValueContainerImpl>> writeValueContainers : containersByDevice.entrySet()) {
				WriteTask writeTask = new WriteTask(this, writeValueContainers.getKey(),
						writeValueContainers.getValue(), writeTasksFinishedSignal);
				newWriteTasks.add(writeTask);
			}
		}
		interrupt();

		try {
			writeTasksFinishedSignal.await();
		} catch (InterruptedException e) {
		}

	}

	@Override
	public void read(List<ReadRecordContainer> readContainers) {
		Map<Device, List<ChannelRecordContainerImpl>> containersByDevice = new HashMap<Device, List<ChannelRecordContainerImpl>>();

		for (ReadRecordContainer container : readContainers) {
			if (container instanceof ChannelRecordContainerImpl == false) {
				throw new IllegalArgumentException("Only use ReadRecordContainer created by Channel.getReadContainer()");
			}

			ChannelImpl channel = (ChannelImpl) container.getChannel();
			List<ChannelRecordContainerImpl> containersOfDevice = containersByDevice
					.get(channel.config.deviceParent.device);
			if (containersOfDevice == null) {
				containersOfDevice = new LinkedList<ChannelRecordContainerImpl>();
				containersByDevice.put(channel.config.deviceParent.device, containersOfDevice);
			}
			containersOfDevice.add((ChannelRecordContainerImpl) container);
		}
		CountDownLatch readTasksFinishedSignal = new CountDownLatch(containersByDevice.size());

		synchronized (newReadTasks) {
			for (Entry<Device, List<ChannelRecordContainerImpl>> channelRecordContainers : containersByDevice
					.entrySet()) {
				ReadTask readTask = new ReadTask(this, channelRecordContainers.getKey(),
						channelRecordContainers.getValue(), readTasksFinishedSignal);
				newReadTasks.add(readTask);
			}
		}
		interrupt();

		try {
			readTasksFinishedSignal.await();
		} catch (InterruptedException e) {
		}
	}

	@Override
	public List<String> getAllIds() {
		List<String> ids = new ArrayList<String>(rootConfig.channelConfigsById.size());
		for (String id : rootConfig.channelConfigsById.keySet()) {
			ids.add(id);
		}
		return ids;
	}

	DataLoggerService getDataLogger() throws DataLoggerNotAvailableException {
		DataLoggerService dataLogger = activeDataLoggers.peekFirst();
		if (dataLogger == null) {
			throw new DataLoggerNotAvailableException();
		}
		logger.debug("Accessing logged values using " + dataLogger.getId());
		return dataLogger;
	}

	@Override
	public DriverInfo getDriverInfo(String driverId) throws DriverNotAvailableException {
		DriverService driver = activeDrivers.get(driverId);
		if (driver == null) {
			throw new DriverNotAvailableException();
		}

		return driver.getInfo();
	}

	@Override
	public DeviceState getDeviceState(String deviceId) {
		DeviceConfigImpl deviceConfig = (DeviceConfigImpl) rootConfig.getDevice(deviceId);
		if (deviceConfig == null) {
			return null;
		}
		return deviceConfig.device.state;
	}

}
