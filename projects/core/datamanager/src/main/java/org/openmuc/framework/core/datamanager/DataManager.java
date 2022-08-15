/*
 * Copyright 2011-2022 Fraunhofer ISE
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
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.apache.felix.service.command.CommandProcessor;
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
import org.openmuc.framework.driver.spi.ChannelRecordContainer;
import org.openmuc.framework.driver.spi.Connection;
import org.openmuc.framework.driver.spi.ConnectionException;
import org.openmuc.framework.driver.spi.DriverDeviceScanListener;
import org.openmuc.framework.driver.spi.DriverService;
import org.openmuc.framework.driver.spi.RecordsReceivedListener;
import org.openmuc.framework.server.spi.ServerMappingContainer;
import org.openmuc.framework.server.spi.ServerService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = { DataAccessService.class, ConfigService.class }, immediate = true, property = {
        CommandProcessor.COMMAND_SCOPE + ":String=openmuc", CommandProcessor.COMMAND_FUNCTION + ":String=reload" })
public final class DataManager extends Thread implements DataAccessService, ConfigService, RecordsReceivedListener {

    private static final String DEFAULT_CONF_FILE = "conf/channels.xml";

    private static final Logger logger = LoggerFactory.getLogger(DataManager.class);
    final LinkedList<Device> connectedDevices = new LinkedList<>();
    final LinkedList<Device> disconnectedDevices = new LinkedList<>();
    final LinkedList<Device> connectionFailures = new LinkedList<>();
    final LinkedList<SamplingTask> samplingTaskFinished = new LinkedList<>();
    final LinkedList<WriteTask> newWriteTasks = new LinkedList<>();
    final LinkedList<ReadTask> newReadTasks = new LinkedList<>();
    final LinkedList<DeviceTask> tasksFinished = new LinkedList<>();
    private final HashMap<String, DriverService> newDrivers = new LinkedHashMap<>();
    private final HashMap<String, ServerService> serverServices = new HashMap<>();
    private final Map<String, DriverService> activeDrivers = new LinkedHashMap<>();
    private final LinkedList<Action> actions = new LinkedList<>();
    private final List<ConfigChangeListener> configChangeListeners = new LinkedList<>();
    private final List<DataLoggerService> newDataLoggers = new LinkedList<>();
    private final Deque<DataLoggerService> activeDataLoggers = new LinkedBlockingDeque<>();
    private final LinkedList<List<ChannelRecordContainer>> receivedRecordContainers = new LinkedList<>();
    private final ReentrantLock configLock = new ReentrantLock();
    CountDownLatch dataLoggerRemovedSignal;
    volatile int activeDeviceCountDown;
    ThreadPoolExecutor executor = null;
    CountDownLatch driverRemovedSignal;
    private volatile boolean stopFlag = false;
    // does not need to be a list because RemovedService() for driver services
    // are never called in parallel:
    private volatile String driverToBeRemovedId = null;
    private volatile DataLoggerService dataLoggerToBeRemoved = null;
    private volatile RootConfigImpl newRootConfigWithoutDefaults = null;
    private volatile RootConfigImpl rootConfig;
    private volatile RootConfigImpl rootConfigWithoutDefaults;
    private File configFile;
    private volatile Boolean dataManagerActivated = false;
    private CountDownLatch newConfigSignal;

    @Activate
    protected void activate() throws TransformerFactoryConfigurationError, IOException, ParserConfigurationException,
            TransformerException, ParseException {

        String configFileName = System.getProperty("org.openmuc.framework.channelconfig");
        if (configFileName == null) {
            configFileName = DEFAULT_CONF_FILE;
        }
        activateWithConfig(new File(configFileName));
    }

    protected void activateWithConfig(File configFile) throws TransformerFactoryConfigurationError, IOException,
            ParserConfigurationException, TransformerException, ParseException {

        logger.info("Activating Data Manager with config {}", configFile);

        NamedThreadFactory namedThreadFactory = new NamedThreadFactory("OpenMUC Data Manager Pool - thread-");
        executor = (ThreadPoolExecutor) Executors.newCachedThreadPool(namedThreadFactory);

        try {
            this.configFile = configFile;
            try {
                rootConfigWithoutDefaults = RootConfigImpl.createFromFile(configFile);
            } catch (FileNotFoundException e) {
                // create an empty configuration and store it in a file
                rootConfigWithoutDefaults = new RootConfigImpl();
                rootConfigWithoutDefaults.writeToFile(configFile);
                logger.info("No configuration file found. Created an empty config file at: {}",
                        configFile.getAbsolutePath());
            } catch (ParseException e) {
                throw new ParseException("Error parsing OpenMUC config file: " + e.getMessage(), e);
            }

            rootConfig = new RootConfigImpl();

            applyConfiguration(rootConfigWithoutDefaults, System.currentTimeMillis());

            start();

            dataManagerActivated = true;
        } catch (ParseException e) {
            logger.error(e.getMessage());
            logger.error("Stopping Framework.");
            final BundleContext bundleContext = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
            try {
                bundleContext.getBundle(0).stop();
                bundleContext.getBundle().stop();
            } catch (BundleException ex) {
                ex.printStackTrace();
            }
        }
    }

    public void reload() {
        logger.info("Reload config from file.");
        try {
            reloadConfigFromFile();
        } catch (FileNotFoundException | ParseException e) {
            logger.error(e.getMessage());
        }
    }

    @Deactivate
    private void deactivate() {
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

            if (actions.isEmpty()) {
                try {
                    while (true) {
                        Thread.sleep(Long.MAX_VALUE);
                    }
                } catch (InterruptedException e) {
                    handleInterruptEvent();
                    continue;
                }
            }

            Action currentAction = actions.getFirst();

            long currentTime = System.currentTimeMillis();

            long elapsedTime = currentTime - currentAction.startTime;

            if (elapsedTime > 1000l) {
                elapsedTimeTooBig(currentAction, currentTime);
                continue;
            }

            long sleepTime = currentAction.startTime - currentTime;
            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    handleInterruptEvent();
                    continue;
                }
            }
            actions.removeFirst();

            if (currentAction.timeouts != null) {
                triggerTimeouts(currentAction.timeouts);
            }

            LoggingController loggingController = new LoggingController(activeDataLoggers);

            if (loggingController.channelsHaveToBeLogged(currentAction)) {
                for (Optional<ChannelCollection> collection : loggingController.triggerLogging(currentAction)) {
                    handleStillFilledChannels(collection, currentAction);
                }
            }

            if (currentAction.connectionRetryDevices != null && !currentAction.connectionRetryDevices.isEmpty()) {
                for (Device device : currentAction.connectionRetryDevices) {
                    device.connectRetrySignal();
                }
            }

            if (currentAction.samplingCollections != null && !currentAction.samplingCollections.isEmpty()) {

                for (ChannelCollection samplingCollection : currentAction.samplingCollections) {
                    List<ChannelRecordContainerImpl> selectedChannels = new ArrayList<>(
                            samplingCollection.channels.size());
                    for (ChannelImpl channel : samplingCollection.channels) {
                        selectedChannels.add(channel.createChannelRecordContainer());
                    }
                    SamplingTask samplingTask = new SamplingTask(this, samplingCollection.device, selectedChannels,
                            samplingCollection.samplingGroup);

                    int timeout = samplingCollection.device.deviceConfig.getSamplingTimeout();

                    boolean taskAddSuccessful = samplingCollection.device.addSamplingTask(samplingTask,
                            samplingCollection.interval);
                    if (taskAddSuccessful && timeout > 0) {
                        addSamplingWorkerTimeoutToActions(samplingTask, currentAction.startTime + timeout);
                    }

                    addSamplingCollectionToActions(samplingCollection,
                            currentAction.startTime + samplingCollection.interval);
                }

            }
        }
    }

    private void handleStillFilledChannels(Optional<ChannelCollection> logCollectionOpt, Action currentAction) {
        if (!logCollectionOpt.isPresent()) {
            return;
        }
        ChannelCollection loggingCollection = logCollectionOpt.get();
        long startTimestamp = currentAction.startTime + loggingCollection.interval;
        addLoggingCollectionToActions(loggingCollection, startTimestamp);
    }

    private void triggerTimeouts(List<SamplingTask> timeouts) {
        for (SamplingTask samplingTask : timeouts) {
            samplingTask.timeout();
        }
    }

    private void elapsedTimeTooBig(Action currentAction, long currentTime) {
        actions.removeFirst();
        logger.error(
                "Action was scheduled for UNIX time {}. But current time is already {}. Will calculate new action time because the action has timed out. Has the system clock jumped?",
                currentAction.startTime, currentTime);

        if (currentAction.timeouts != null) {
            triggerTimeouts(currentAction.timeouts);
        }
        if (currentAction.loggingCollections != null) {
            for (ChannelCollection loggingCollection : currentAction.loggingCollections) {
                long startTimestamp = loggingCollection.calculateNextActionTime(currentTime);
                addLoggingCollectionToActions(loggingCollection, startTimestamp);
            }
        }
        if (currentAction.samplingCollections != null) {
            for (ChannelCollection samplingCollection : currentAction.samplingCollections) {
                long startTimestamp = samplingCollection.calculateNextActionTime(currentTime);
                addSamplingCollectionToActions(samplingCollection, startTimestamp);
            }
        }
        if (currentAction.connectionRetryDevices != null) {
            for (Device device : currentAction.connectionRetryDevices) {
                long startTimestamp = currentTime + device.deviceConfig.getConnectRetryInterval();
                addReconnectDeviceToActions(device, startTimestamp);
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
                    fittingAction.samplingCollections = new LinkedList<>();
                }
                break;
            }
            else if (currentAction.startTime > startTimestamp) {
                fittingAction = new Action(startTimestamp);
                fittingAction.samplingCollections = new LinkedList<>();
                actionIterator.previous();
                actionIterator.add(fittingAction);
                break;
            }
        }

        if (fittingAction == null) {
            fittingAction = new Action(startTimestamp);
            fittingAction.samplingCollections = new LinkedList<>();
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
                    fittingAction.loggingCollections = new LinkedList<>();
                }
                break;
            }
            else if (currentAction.startTime > startTimestamp) {
                fittingAction = new Action(startTimestamp);
                fittingAction.loggingCollections = new LinkedList<>();
                actionIterator.previous();
                actionIterator.add(fittingAction);
                break;
            }
        }

        if (fittingAction == null) {
            fittingAction = new Action(startTimestamp);
            fittingAction.loggingCollections = new LinkedList<>();
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
                    fittingAction.connectionRetryDevices = new LinkedList<>();
                }
                break;
            }

            if (currentAction.startTime > startTimestamp) {
                fittingAction = new Action(startTimestamp);
                fittingAction.connectionRetryDevices = new LinkedList<>();
                actionIterator.previous();
                actionIterator.add(fittingAction);
                break;
            }
        }

        if (fittingAction == null) {
            fittingAction = new Action(startTimestamp);
            fittingAction.connectionRetryDevices = new LinkedList<>();
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
                    fittingAction.timeouts = new LinkedList<>();
                }
                break;
            }
            else if (currentAction.startTime > timeout) {
                fittingAction = new Action(timeout);
                fittingAction.timeouts = new LinkedList<>();
                actionIterator.previous();
                actionIterator.add(fittingAction);
                break;
            }
        }

        if (fittingAction == null) {
            fittingAction = new Action(timeout);
            fittingAction.timeouts = new LinkedList<>();
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
            List<ChannelRecordContainer> recordContainers;
            LoggingController loggingController = new LoggingController(activeDataLoggers);
            List<ChannelRecordContainerImpl> channelRecordContainerList = new ArrayList<>();
            while ((recordContainers = receivedRecordContainers.poll()) != null) {
                recordContainers.stream()
                        .map(recContainer -> (ChannelRecordContainerImpl) recContainer)
                        .filter(containerImpl -> containerImpl.getChannel().getChannelState() == ChannelState.LISTENING
                                || containerImpl.getChannel().getDriverName().equals("virtual"))
                        .forEach(containerImpl -> {
                            containerImpl.getChannel().setNewRecord(containerImpl.getRecord());
                            if (containerImpl.getChannel().isLoggingEvent()) {
                                channelRecordContainerList.add(containerImpl);
                            }
                        });
            }
            loggingController.deliverLogsToEventBasedLogServices(channelRecordContainerList);
        }

        synchronized (samplingTaskFinished) {
            SamplingTask samplingTask;
            while ((samplingTask = samplingTaskFinished.poll()) != null) {
                samplingTask.storeValues();
                samplingTask.device.taskFinished();
            }
        }

        synchronized (tasksFinished) {
            DeviceTask deviceTask;
            while ((deviceTask = tasksFinished.poll()) != null) {
                deviceTask.device.taskFinished();

            }
        }

        synchronized (newDrivers) {
            // needed to synchronize with getRunningDrivers
            synchronized (activeDrivers) {
                activeDrivers.putAll(newDrivers);
            }

            for (Entry<String, DriverService> newDriverEntry : newDrivers.entrySet()) {
                String driverId = newDriverEntry.getKey();
                logger.info("Driver registered: " + driverId);

                DriverConfigImpl driverConfig = rootConfig.driverConfigsById.get(driverId);

                if (driverConfig == null) {
                    continue;
                }

                driverConfig.activeDriver = newDriverEntry.getValue();
                for (DeviceConfigImpl deviceConfig : driverConfig.deviceConfigsById.values()) {
                    deviceConfig.device.driverRegisteredSignal();
                }
            }
            newDrivers.clear();
        }

        synchronized (newDataLoggers) {
            if (!newDataLoggers.isEmpty()) {
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
            if (!activeDataLoggers.remove(dataLoggerToBeRemoved)) {
                newDataLoggers.remove(dataLoggerToBeRemoved);
            }
            dataLoggerToBeRemoved = null;
            dataLoggerRemovedSignal.countDown();
        }

        synchronized (connectionFailures) {
            if (currentTime == 0) {
                currentTime = System.currentTimeMillis();
            }
            Device connectionFailureDevice;
            while ((connectionFailureDevice = connectionFailures.poll()) != null) {
                connectionFailureDevice.connectFailureSignal(currentTime);
            }
        }

        synchronized (connectedDevices) {
            if (currentTime == 0) {
                currentTime = System.currentTimeMillis();
            }
            Device connectedDevice;
            while ((connectedDevice = connectedDevices.poll()) != null) {
                connectedDevice.connectedSignal(currentTime);
            }
        }

        synchronized (newWriteTasks) {
            addTasksAndClear(newWriteTasks);
        }

        synchronized (newReadTasks) {
            addTasksAndClear(newReadTasks);
        }

        synchronized (disconnectedDevices) {
            Device connectedDevice;
            while ((connectedDevice = disconnectedDevices.poll()) != null) {
                connectedDevice.disconnectedSignal();
            }
        }

    }

    private <T extends DeviceTask & ConnectedTask> void addTasksAndClear(Queue<T> newTasksList) {
        T nextTask;
        while ((nextTask = newTasksList.poll()) != null) {
            nextTask.device.addTask(nextTask);
        }
    }

    private void applyConfiguration(RootConfigImpl configWithoutDefaults, long currentTime) {

        RootConfigImpl newRootConfig = configWithoutDefaults.cloneWithDefaults();

        List<LogChannel> logChannels = new LinkedList<>();

        for (DriverConfigImpl oldDriverConfig : rootConfig.driverConfigsById.values()) {
            DriverConfigImpl newDriverConfig = newRootConfig.driverConfigsById.get(oldDriverConfig.id);
            if (newDriverConfig != null) {
                newDriverConfig.activeDriver = oldDriverConfig.activeDriver;
            }
            for (DeviceConfigImpl oldDeviceConfig : oldDriverConfig.deviceConfigsById.values()) {
                DeviceConfigImpl newDeviceConfig = null;
                if (newDriverConfig != null) {
                    newDeviceConfig = newDriverConfig.deviceConfigsById.get(oldDeviceConfig.getId());
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
                    oldDeviceConfig = oldDriverConfig.deviceConfigsById.get(newDeviceConfig.getId());
                }

                if (oldDeviceConfig == null) {
                    // Device is new
                    newDeviceConfig.device = new Device(this, newDeviceConfig, currentTime, logChannels);
                    if (newDeviceConfig.device.getState() == DeviceState.CONNECTING) {
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

        updateLogChannelsInDataLoggers(logChannels);

        newRootConfig.logChannels = logChannels;

        synchronized (configChangeListeners) {

            rootConfig = newRootConfig;
            rootConfigWithoutDefaults = configWithoutDefaults;

            for (final ConfigChangeListener configChangeListener : configChangeListeners) {
                if (configChangeListener == null) {
                    continue;
                }
                executor.execute(configChangeListener::configurationChanged);
            }
        }

        notifyServers();
    }

    private void updateLogChannelsInDataLoggers(List<LogChannel> logChannels) {
        for (DataLoggerService dataLogger : activeDataLoggers) {
            if (dataLogger.logSettingsRequired()) {
                setLoggerSpecific(dataLogger, logChannels);
            }
            else {
                setLoggerSpecificAndWithoutSettings(dataLogger, logChannels);
            }
        }
    }

    private void setLoggerSpecific(DataLoggerService dataLogger, List<LogChannel> logChannels) {
        List<LogChannel> specificLogChannels = filterLogChannelsForSpecificLogger(dataLogger.getId(), logChannels);
        dataLogger.setChannelsToLog(specificLogChannels);
    }

    private void setLoggerSpecificAndWithoutSettings(DataLoggerService dataLogger, List<LogChannel> logChannels) {
        List<LogChannel> specificLogChannels = filterLogChannelsForSpecificLogger(dataLogger.getId(), logChannels);
        List<LogChannel> logChannelsWithoutLoggingSettings = logChannels.stream()
                .filter(logChannel -> logChannel.getLoggingSettings() == null
                        || logChannel.getLoggingSettings().isEmpty())
                .collect(Collectors.toList());

        specificLogChannels.addAll(logChannelsWithoutLoggingSettings);
        dataLogger.setChannelsToLog(logChannelsWithoutLoggingSettings);
    }

    private List<LogChannel> filterLogChannelsForSpecificLogger(String loggerId, List<LogChannel> logChannels) {
        return logChannels.stream()
                .filter(logChannel -> logChannel.getLoggingSettings() != null
                        && !logChannel.getLoggingSettings().isEmpty())
                .filter(logChannel -> parseDefinedLogger(logChannel.getLoggingSettings()).contains(loggerId))
                .collect(Collectors.toList());
    }

    private List<String> parseDefinedLogger(String logSettings) {
        String[] loggerSegments = logSettings.split(";");
        List<String> definedLogger = Arrays.stream(loggerSegments)
                .map(seg -> seg.split(":")[0])
                .collect(Collectors.toList());

        return definedLogger;
    }

    void addToSamplingCollections(ChannelImpl channel, Long time) {

        ChannelCollection fittingSamplingCollection = null;
        for (Action action : actions) {
            if (action.samplingCollections != null) {
                for (ChannelCollection samplingCollection : action.samplingCollections) {
                    if (samplingCollection.interval == channel.getSamplingInterval()
                            && samplingCollection.timeOffset == channel.getSamplingTimeOffset()
                            && samplingCollection.samplingGroup.equals(channel.config.getSamplingGroup())
                            && samplingCollection.device == channel.config.deviceParent.device) {
                        fittingSamplingCollection = samplingCollection;
                        break;
                    }
                }
            }
        }

        if (fittingSamplingCollection == null) {
            fittingSamplingCollection = new ChannelCollection(channel.getSamplingInterval(),
                    channel.getSamplingTimeOffset(), channel.config.getSamplingGroup(),
                    channel.config.deviceParent.device);
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
                    if (loggingCollection.interval == channel.getLoggingInterval()
                            && loggingCollection.timeOffset == channel.getLoggingTimeOffset()) {
                        fittingLoggingCollection = loggingCollection;
                        break;
                    }
                }
            }
        }
        if (fittingLoggingCollection == null) {
            fittingLoggingCollection = new ChannelCollection(channel.getLoggingInterval(),
                    channel.getLoggingTimeOffset(), null, null);
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
        if (channel.loggingCollection.channels.isEmpty()) {
            channel.loggingCollection.action.loggingCollections.remove(channel.loggingCollection);
        }
        channel.loggingCollection = null;
    }

    void removeFromSamplingCollections(ChannelImpl channel) {
        channel.samplingCollection.channels.remove(channel);
        if (channel.samplingCollection.channels.isEmpty()) {
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

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    void bindDriverService(DriverService driver) {

        String driverId = driver.getInfo().getId();

        synchronized (newDrivers) {
            if (activeDrivers.get(driverId) != null || newDrivers.get(driverId) != null) {
                logger.error("Unable to register driver: a driver with the ID {}  is already registered.", driverId);
                return;
            }
            newDrivers.put(driverId, driver);
            interrupt();
        }
    }

    /**
     * Registers a new ServerService.
     *
     * @param serverService
     *            ServerService object to register
     */
    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    private void bindServerService(ServerService serverService) {
        String serverId = serverService.getId();
        serverServices.put(serverId, serverService);

        if (dataManagerActivated) {
            notifyServer(serverService);
        }

        logger.info("Registered Server: {}.", serverId);
    }

    /**
     * Removes a registered ServerService.
     *
     * @param serverService
     *            ServerService object to unset
     */
    @SuppressWarnings("unused")
    private void unbindServerService(ServerService serverService) {
        serverServices.remove(serverService.getId());
    }

    /**
     * Updates all ServerServices with mapped channels.
     */
    protected void notifyServers() {
        for (ServerService serverService : serverServices.values()) {
            notifyServer(serverService);
        }
    }

    /**
     * Updates a specified ServerService with mapped channels.
     *
     * @param serverService
     *            ServerService object to updating
     */
    protected void notifyServer(ServerService serverService) {
        List<ServerMappingContainer> relatedServerMappings = new ArrayList<>();

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

    @SuppressWarnings("unused")
    private void unbindDriverService(DriverService driver) {

        String driverId = driver.getInfo().getId();
        logger.info("Unregistering driver: {}.", driverId);

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
        logger.info("Driver unregistered: {}", driverId);
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    void bindDataLoggerService(DataLoggerService dataLogger) {
        synchronized (newDataLoggers) {
            newDataLoggers.add(dataLogger);
            interrupt();
        }
    }

    @SuppressWarnings("unused")
    private void unbindDataLoggerService(DataLoggerService dataLogger) {

        String dataLoggerId = dataLogger.getId();
        logger.info("Unregistering data logger: {}.", dataLoggerId);

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
            if (!activeDataLoggers.remove(dataLogger)) {
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
        if (rootConfig == null) {
            return null;
        }
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
        List<ChannelRecordContainer> recordContainersCopy = new ArrayList<>(recordContainers.size());
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
            DeviceConfigImpl deviceConfigImpl = (DeviceConfigImpl) deviceConfig;
            if (deviceConfigImpl.device.connection != connection) {
                continue;
            }

            Device device = deviceConfigImpl.device;
            logger.info("Connection to device {} was interrupted.", device.deviceConfig.getId());
            device.disconnectedSignal();
            return;
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
        return new RootConfigImpl(this.rootConfigWithoutDefaults);
    }

    @Override
    public void setConfig(RootConfig config) {
        configLock.lock();
        try {
            RootConfigImpl newConfigCopy = new RootConfigImpl((RootConfigImpl) config);
            setNewConfig(newConfigCopy);
        } finally {
            configLock.unlock();
        }

    }

    @Override
    public RootConfig getConfig(ConfigChangeListener listener) {
        synchronized (configChangeListeners) {
            for (ConfigChangeListener configChangeListener : configChangeListeners) {
                if (configChangeListener == listener) {
                    configChangeListeners.remove(configChangeListener);
                }
            }
            configChangeListeners.add(listener);
            return getConfig();
        }
    }

    @Override
    public void stopListeningForConfigChange(ConfigChangeListener listener) {
        synchronized (configChangeListeners) {
            this.configChangeListeners.remove(listener);
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

    @Override
    public List<DeviceScanInfo> scanForDevices(String driverId, String settings)
            throws DriverNotAvailableException, ArgumentSyntaxException, ScanException, ScanInterruptedException {

        DriverService driver = activeDrivers.get(driverId);
        if (driver == null) {
            throw new DriverNotAvailableException();
        }

        BlockingScanListener blockingScanListener = new BlockingScanListener();

        try {
            driver.scanForDevices(settings, blockingScanListener);
        } catch (RuntimeException e) {
            if (e instanceof UnsupportedOperationException) {
                throw e;
            }

            throw new ScanException(e);
        }

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
    public List<ChannelScanInfo> scanForChannels(String deviceId, String settings)
            throws DriverNotAvailableException, UnsupportedOperationException, ArgumentSyntaxException, ScanException {
        // TODO this function is probably not thread safe

        DeviceConfigImpl config = (DeviceConfigImpl) this.rootConfig.getDevice(deviceId);
        if (config == null) {
            throw new ScanException("No device with ID \"" + deviceId + "\" found.");
        }
        DriverService activeDriver = activeDrivers.get(config.driverParent.id);
        if (activeDriver == null) {
            throw new DriverNotAvailableException();
        }

        waitTilDeviceIsConnected(config.device);

        try {
            return config.device.connection.scanForChannels(settings);
        } catch (ConnectionException e) {
            config.device.disconnectedSignal();
            throw new ScanException(e.getMessage(), e);
        }
    }

    private void waitTilDeviceIsConnected(Device device) throws ScanException {
        for (int i = 0; i < 10 && device.getState() == DeviceState.CONNECTING; i++) {
            try {
                Thread.sleep(10L);
            } catch (InterruptedException e) {
                // ignore
            }
        }

        if (device.connection == null) {
            throw new ScanException("Connection of the device is not yet initialized.");
        }
    }

    @Override
    public List<String> getIdsOfRunningDrivers() {
        List<String> availableDrivers;
        synchronized (activeDrivers) {
            availableDrivers = new ArrayList<>(activeDrivers.size());
            for (String activeDriverName : activeDrivers.keySet()) {
                availableDrivers.add(activeDriverName);
            }
        }
        return availableDrivers;
    }

    @Override
    public void write(List<WriteValueContainer> values) {
        HashMap<Device, List<WriteValueContainerImpl>> containersByDevice = new LinkedHashMap<>();

        for (WriteValueContainer value : values) {
            WriteValueContainerImpl valueContainerImpl = (WriteValueContainerImpl) value;

            if (valueContainerImpl.getValue() == null) {
                valueContainerImpl.setFlag(Flag.CANNOT_WRITE_NULL_VALUE);
                continue;
            }

            Device device = valueContainerImpl.getChannel().config.deviceParent.device;
            List<WriteValueContainerImpl> writeValueContainers = containersByDevice.get(device);

            if (writeValueContainers == null) {
                writeValueContainers = new LinkedList<>();
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
        Map<Device, List<ChannelRecordContainerImpl>> containersByDevice = new HashMap<>();

        for (ReadRecordContainer container : readContainers) {
            if (!(container instanceof ChannelRecordContainerImpl)) {
                throw new IllegalArgumentException(
                        "Only use ReadRecordContainer created by Channel.getReadContainer()");
            }

            ChannelImpl channel = (ChannelImpl) container.getChannel();
            List<ChannelRecordContainerImpl> containersOfDevice = containersByDevice
                    .get(channel.config.deviceParent.device);
            if (containersOfDevice == null) {
                containersOfDevice = new LinkedList<>();
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
        return new ArrayList<>(rootConfig.channelConfigsById.keySet());
    }

    DataLoggerService getDataLogger(String loggerId) throws DataLoggerNotAvailableException {
        DataLoggerService dataLogger;
        if (loggerId == null || loggerId.isEmpty()) {
            dataLogger = activeDataLoggers.peekFirst();
        }
        else {
            dataLogger = activeDataLoggers.stream()
                    .filter(activeLogger -> activeLogger.getId().equals(loggerId))
                    .findFirst()
                    .orElseThrow(() -> {
                        logger.warn("DataLogger with id " + loggerId + " not found for reading logs!");
                        return new DataLoggerNotAvailableException();
                    });
        }

        logger.debug("Accessing logged values using {}", dataLogger.getId());

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
        return deviceConfig.device.getState();
    }

    class BlockingScanListener implements DriverDeviceScanListener {
        List<DeviceScanInfo> scanInfos = new ArrayList<>();

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

}
