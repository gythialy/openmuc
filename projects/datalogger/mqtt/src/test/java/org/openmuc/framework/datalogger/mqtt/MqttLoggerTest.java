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

package org.openmuc.framework.datalogger.mqtt;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.openmuc.framework.datalogger.spi.LogChannel;
import org.openmuc.framework.datalogger.spi.LoggingRecord;
import org.openmuc.framework.lib.mqtt.MqttConnection;
import org.openmuc.framework.lib.mqtt.MqttSettings;
import org.openmuc.framework.lib.mqtt.MqttWriter;
import org.openmuc.framework.lib.osgi.config.PropertyHandler;

//FIXME provide unit tests
public class MqttLoggerTest {

    // FIXME
    @Disabled
    @Test
    void testDifferentTopics() {
        MqttLogger logger = new MqttLogger();

        LogChannel logChannelMock1 = Mockito.mock(LogChannel.class);
        Mockito.when(logChannelMock1.getId()).thenReturn("Channel1");
        Mockito.when(logChannelMock1.getLoggingSettings()).thenReturn("topic1");

        LogChannel logChannelMock2 = Mockito.mock(LogChannel.class);
        Mockito.when(logChannelMock2.getId()).thenReturn("Channel2");
        Mockito.when(logChannelMock2.getLoggingSettings()).thenReturn("topic2");

        List<LogChannel> channels = new ArrayList<>();
        channels.add(logChannelMock1);
        channels.add(logChannelMock2);

        logger.setChannelsToLog(channels);

        Dictionary<String, String> dict = new Hashtable<>();
        dict.put(MqttLoggerSettings.PORT, "1883");
        dict.put(MqttLoggerSettings.HOST, "localhost");
        dict.put(MqttLoggerSettings.SSL, "false");
        dict.put(MqttLoggerSettings.USERNAME, "");
        dict.put(MqttLoggerSettings.PASSWORD, "");
        dict.put(MqttLoggerSettings.PARSER, "openmuc");
        dict.put(MqttLoggerSettings.MULTIPLE, "false");
        dict.put(MqttLoggerSettings.MAX_FILE_COUNT, "1");
        dict.put(MqttLoggerSettings.MAX_FILE_SIZE, "2000");
        dict.put(MqttLoggerSettings.MAX_BUFFER_SIZE, "100");

        logger.updated(dict);

        List<LoggingRecord> records = new ArrayList<>();
        records.add(new LoggingRecord("Channel1", null));
        records.add(new LoggingRecord("Channel2", null));
        logger.log(records, System.currentTimeMillis());

        Mockito.verify(records.get(0), Mockito.times(0)).getChannelId();

    }

    // @BeforeAll
    static void connect() {
        String packageName = MqttLogger.class.getPackage().getName().toLowerCase();
        System.setProperty(packageName + ".host", "localhost");
        System.setProperty(packageName + ".port", "1883");
        System.setProperty(packageName + ".username", "guest");
        System.setProperty(packageName + ".password", "guest");
        System.setProperty(packageName + ".topic", "device/data");

        System.setProperty(packageName + ".maxFileCount", "2");
        System.setProperty(packageName + ".maxFileSize", "1");
        System.setProperty(packageName + ".maxBufferSize", "1");

        String pid = MqttLogger.class.getName();
        MqttLoggerSettings settings = new MqttLoggerSettings();
        PropertyHandler propertyHandler = new PropertyHandler(settings, pid);
        MqttSettings Mqttsettings = new MqttSettings(propertyHandler.getString(MqttLoggerSettings.HOST),
                propertyHandler.getInt(MqttLoggerSettings.PORT), propertyHandler.getString(MqttLoggerSettings.USERNAME),
                propertyHandler.getString(MqttLoggerSettings.PASSWORD),
                propertyHandler.getBoolean(MqttLoggerSettings.SSL),
                propertyHandler.getInt(MqttLoggerSettings.MAX_BUFFER_SIZE),
                propertyHandler.getInt(MqttLoggerSettings.MAX_FILE_SIZE),
                propertyHandler.getInt(MqttLoggerSettings.MAX_FILE_COUNT),
                propertyHandler.getInt(MqttLoggerSettings.CONNECTION_RETRY_INTERVAL),
                propertyHandler.getInt(MqttLoggerSettings.CONNECTION_ALIVE_INTERVAL),
                propertyHandler.getString(MqttLoggerSettings.PERSISTENCE_DIRECTORY));
        MqttConnection connection = new MqttConnection(Mqttsettings);
        MqttWriter mqttWriter = new MqttWriter(connection, "mqttlogger");
        mqttWriter.getConnection().connect();
    }

    /**
     * Complete test of file buffering from logger's point of view.
     * <p>
     * Scenario: Logger connects to a broker, after some while connection to broker is interrupted. Now, logger should
     * log into a file. After some time the connection to the broker is reestablished. Now logger should transfer all
     * buffered messages to the broker and clear the file (buffer) afterwards. At the same time new live logs should be
     * send to the broker as well (in parallel)
     */
    @Disabled
    @Test
    void testFileBuffering() {

        // Involves: mqtt logger, lib-mqtt, lib-FilePersistence
        // Note: lib-FilePersistence has it own tests for correct parameter handling

        // 1. start logger and connect to a BrokerMock (just print messages to terminal)
        // (executor which calls log every second)

        // 2. log a few messages to terminal

        // 3. interrupt/close connection of BrokerMock

        // 4. logger should log into file. check if it does.

        // 5. reconnect to the BrokerMock

        // 6. empty file buffer and send (historical) messages to broker AND send live log messages to broker as well
    }
}
