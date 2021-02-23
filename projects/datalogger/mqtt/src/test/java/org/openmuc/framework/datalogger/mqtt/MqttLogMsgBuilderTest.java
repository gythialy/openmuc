/*
 * Copyright 2011-2021 Fraunhofer ISE
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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.openmuc.framework.data.DoubleValue;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.datalogger.mqtt.dto.MqttLogChannel;
import org.openmuc.framework.datalogger.mqtt.dto.MqttLogMsg;
import org.openmuc.framework.datalogger.mqtt.util.MqttLogMsgBuilder;
import org.openmuc.framework.datalogger.spi.LogChannel;
import org.openmuc.framework.datalogger.spi.LoggingRecord;
import org.openmuc.framework.lib.parser.openmuc.OpenmucParserServiceImpl;

/**
 * Test checks if the correct log messages are generated for a given set of loggingRecords and given logger settings
 */
// FIXME refactor to remove some code duplication
public class MqttLogMsgBuilderTest {

    private static LogChannel logChannelMockA;
    private static LogChannel logChannelMockB;
    private static LogChannel logChannelMockC;
    private static LogChannel logChannelMockD;

    private static Record record3;
    private static Record record5;
    private static Record record7;

    private static OpenmucParserServiceImpl parser;
    private static final long TIMESTAMP = 1599122299230L;

    private static final String TOPIC_1 = "topic1";
    private static final String TOPIC_2 = "topic2";

    // set true when developing tests, set false for gradle to avoid debug messages on "gradle build"
    private final boolean isDebugEnabled = false;

    @BeforeAll
    public static void setup() {
        initChannelMocks();
        initDummyRecords();
        parser = new OpenmucParserServiceImpl();
    }

    private static void initDummyRecords() {
        // some dummy records
        record3 = new Record(new DoubleValue(3.0), TIMESTAMP);
        record5 = new Record(new DoubleValue(5.0), TIMESTAMP);
        record7 = new Record(new DoubleValue(7.0), TIMESTAMP);
    }

    private static void initChannelMocks() {
        // prepare some channels for tests
        logChannelMockA = Mockito.mock(LogChannel.class);
        Mockito.when(logChannelMockA.getId()).thenReturn("ChannelA");
        Mockito.when(logChannelMockA.getLoggingSettings()).thenReturn("mqttlogger:topic=" + TOPIC_1);

        // NOTE: same topic as channel1
        logChannelMockB = Mockito.mock(LogChannel.class);
        Mockito.when(logChannelMockB.getId()).thenReturn("ChannelB");
        Mockito.when(logChannelMockB.getLoggingSettings()).thenReturn("mqttlogger:topic=" + TOPIC_1);

        logChannelMockC = Mockito.mock(LogChannel.class);
        Mockito.when(logChannelMockC.getId()).thenReturn("ChannelC");
        Mockito.when(logChannelMockC.getLoggingSettings()).thenReturn("mqttlogger:topic=" + TOPIC_2);

        logChannelMockD = Mockito.mock(LogChannel.class);
        Mockito.when(logChannelMockD.getId()).thenReturn("ChannelD");
        Mockito.when(logChannelMockD.getLoggingSettings()).thenReturn("mqttlogger:topic=topic4");
    }

    @Test
    public void test_logSingleChannel_multipleFalse() {

        // 1. prepare channels to log - equal to logger.setChannelsToLog(...) call
        HashMap<String, MqttLogChannel> channelsToLog = new HashMap<>();
        channelsToLog.put(logChannelMockA.getId(), new MqttLogChannel(logChannelMockA));

        // 2. apply settings to logger
        boolean isLogMultiple = false;

        // 3. prepare records which should be logged
        List<LoggingRecord> records = new ArrayList<>();
        records.add(new LoggingRecord(logChannelMockA.getId(), record3));

        // 4. equal to calling logger.log(..) method
        MqttLogMsgBuilder builder = new MqttLogMsgBuilder(channelsToLog, parser);
        List<MqttLogMsg> messages = builder.buildLogMsg(records, isLogMultiple);

        printDebug(isDebugEnabled, messages);

        String controlString = TOPIC_1 + ": {\"timestamp\":" + TIMESTAMP + ",\"flag\":\"VALID\",\"value\":3.0}";
        assertEquals(controlString, TOPIC_1 + ": " + new String(messages.get(0).message));
    }

    @Test
    public void test_logSingleChannel_multipleTrue() {

        // 1. prepare channels to log - equal to logger.setChannelsToLog(...) call
        HashMap<String, MqttLogChannel> channelsToLog = new HashMap<>();
        channelsToLog.put(logChannelMockA.getId(), new MqttLogChannel(logChannelMockA));

        // 2. apply settings to logger
        boolean isLogMultiple = true;

        // 3. prepare records which should be logged
        List<LoggingRecord> records = new ArrayList<>();
        records.add(new LoggingRecord(logChannelMockA.getId(), record3));

        // 4. equal to calling logger.log(..) method
        MqttLogMsgBuilder builder = new MqttLogMsgBuilder(channelsToLog, parser);
        List<MqttLogMsg> messages = builder.buildLogMsg(records, isLogMultiple);

        printDebug(isDebugEnabled, messages);

        String controlString = TOPIC_1 + ": {\"timestamp\":" + TIMESTAMP + ",\"flag\":\"VALID\",\"value\":3.0}" + "\n";
        assertEquals(controlString, TOPIC_1 + ": " + new String(messages.get(0).message));
    }

    @Test
    public void test_logTwoChannels_sameTopic_multipleFalse() {

        // 1. prepare channels to log - equal to logger.setChannelsToLog(...) call
        HashMap<String, MqttLogChannel> channelsToLog = new HashMap<>();
        channelsToLog.put(logChannelMockA.getId(), new MqttLogChannel(logChannelMockA));
        channelsToLog.put(logChannelMockB.getId(), new MqttLogChannel(logChannelMockB));

        // 2. apply settings to logger
        boolean isLogMultiple = false;

        // 3. prepare records which should be logged
        List<LoggingRecord> records = new ArrayList<>();
        records.add(new LoggingRecord(logChannelMockA.getId(), record3));
        records.add(new LoggingRecord(logChannelMockB.getId(), record5));

        // 4. equal to calling logger.log(..) method
        MqttLogMsgBuilder builder = new MqttLogMsgBuilder(channelsToLog, parser);
        List<MqttLogMsg> messages = builder.buildLogMsg(records, isLogMultiple);

        printDebug(isDebugEnabled, messages);

        // expected size = 2 since isLogMultiple = false;
        assertEquals(2, messages.size());

        // check content of the messages
        String referenceString1 = TOPIC_1 + ": {\"timestamp\":" + TIMESTAMP + ",\"flag\":\"VALID\",\"value\":3.0}";
        String referenceString2 = TOPIC_1 + ": {\"timestamp\":" + TIMESTAMP + ",\"flag\":\"VALID\",\"value\":5.0}";

        assertEquals(referenceString1, TOPIC_1 + ": " + new String(messages.get(0).message));
        assertEquals(referenceString2, TOPIC_1 + ": " + new String(messages.get(1).message));
    }

    @Test
    public void test_logTwoChannels_sameTopic_multipleTrue() {

        // 1. prepare channels to log - equal to logger.setChannelsToLog(...) call
        HashMap<String, MqttLogChannel> channelsToLog = new HashMap<>();
        channelsToLog.put(logChannelMockA.getId(), new MqttLogChannel(logChannelMockA));
        channelsToLog.put(logChannelMockB.getId(), new MqttLogChannel(logChannelMockB));

        // 2. apply settings to logger
        boolean isLogMultiple = true;

        // 3. prepare records which should be logged
        List<LoggingRecord> records = new ArrayList<>();
        records.add(new LoggingRecord(logChannelMockA.getId(), record3));
        records.add(new LoggingRecord(logChannelMockB.getId(), record5));

        // 4. equal to calling logger.log(..) method
        MqttLogMsgBuilder builder = new MqttLogMsgBuilder(channelsToLog, parser);
        List<MqttLogMsg> messages = builder.buildLogMsg(records, isLogMultiple);

        printDebug(isDebugEnabled, messages);

        // expected size = 1 since isLogMultiple = true;
        assertEquals(1, messages.size());

        // check content of the messages
        StringBuilder sbRef = new StringBuilder();
        sbRef.append(TOPIC_1 + ": {\"timestamp\":" + TIMESTAMP + ",\"flag\":\"VALID\",\"value\":3.0}");
        sbRef.append("\n");
        sbRef.append("{\"timestamp\":" + TIMESTAMP + ",\"flag\":\"VALID\",\"value\":5.0}");
        sbRef.append("\n");
        String referenceString = sbRef.toString();

        StringBuilder sbTest = new StringBuilder();
        sbTest.append(TOPIC_1 + ": ").append(new String(messages.get(0).message));
        String testString = sbTest.toString();

        assertEquals(referenceString, testString);
    }

    @Test
    public void test_logTwoChannels_differentTopic_multipleTrue() {

        // 1. prepare channels to log - equal to logger.setChannelsToLog(...) call
        HashMap<String, MqttLogChannel> channelsToLog = new HashMap<>();
        channelsToLog.put(logChannelMockA.getId(), new MqttLogChannel(logChannelMockA));
        channelsToLog.put(logChannelMockC.getId(), new MqttLogChannel(logChannelMockC));

        // 2. apply settings to logger
        boolean isLogMultiple = true;

        // 3. prepare records which should be logged
        List<LoggingRecord> records = new ArrayList<>();
        records.add(new LoggingRecord(logChannelMockA.getId(), record3));
        records.add(new LoggingRecord(logChannelMockC.getId(), record7));

        // 4. equal to calling logger.log(..) method
        MqttLogMsgBuilder builder = new MqttLogMsgBuilder(channelsToLog, parser);

        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            List<MqttLogMsg> messages = builder.buildLogMsg(records, isLogMultiple);
        });

    }

    private void printDebug(boolean isEnabled, List<MqttLogMsg> messages) {
        if (isEnabled) { // enable/disable debug output
            int i = 1;
            for (MqttLogMsg msg : messages) {
                System.out.println("msgNr " + i++);
                System.out.println(msg.topic + " " + new String(msg.message));
            }
        }
    }
}
