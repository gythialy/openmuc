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

package org.openmuc.framework.lib.parser.openmuc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openmuc.framework.data.ByteArrayValue;
import org.openmuc.framework.data.DoubleValue;
import org.openmuc.framework.data.Flag;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.data.StringValue;
import org.openmuc.framework.data.Value;
import org.openmuc.framework.data.ValueType;
import org.openmuc.framework.datalogger.spi.LoggingRecord;
import org.openmuc.framework.parser.spi.ParserService;
import org.openmuc.framework.parser.spi.SerializationException;

/**
 * ToDo: add more tests for different datatypes
 */
class OpenmucParserServiceImplTest {

    private ParserService parserService;

    @BeforeEach
    private void setupService() {
        parserService = new OpenmucParserServiceImpl();
    }

    @Test
    void serializeMultipleRecords() throws SerializationException {

        StringBuilder sb = new StringBuilder();
        sb.append("{\"timestamp\":1582722316,\"flag\":\"VALID\",\"value\":3.0}");
        sb.append("\n");
        sb.append("{\"timestamp\":1582722316,\"flag\":\"VALID\",\"value\":5.0}");
        sb.append("\n");
        String controlString = sb.toString();

        Value doubleValue1 = new DoubleValue(3.0);
        long timestamp1 = 1582722316;
        Flag flag1 = Flag.VALID;
        Record record1 = new Record(doubleValue1, timestamp1, flag1);

        Value doubleValue2 = new DoubleValue(5.0);
        long timestamp2 = 1582722316;
        Flag flag2 = Flag.VALID;
        Record record2 = new Record(doubleValue2, timestamp2, flag2);

        List<LoggingRecord> openMucRecords = new ArrayList<>();
        openMucRecords.add(new LoggingRecord("channel1", record1));
        openMucRecords.add(new LoggingRecord("channel2", record2));

        byte[] serializedRecord = parserService.serialize(openMucRecords);
        String serializedJson = new String(serializedRecord);
        assertEquals(controlString, serializedJson);
    }

    @Test
    void serializeDoubleValue() throws SerializationException {
        String controlString = "{\"timestamp\":1582722316,\"flag\":\"VALID\",\"value\":3.0}";
        Value doubleValue = new DoubleValue(3.0);
        long timestamp = 1582722316;
        Flag flag = Flag.VALID;
        Record record = new Record(doubleValue, timestamp, flag);
        byte[] serializedRecord = parserService.serialize(new LoggingRecord("test", record));
        String serializedJson = new String(serializedRecord);
        assertEquals(controlString, serializedJson);
    }

    @Test
    void serializeStringValue() throws SerializationException {
        String controlString = "{\"timestamp\":1582722316,\"flag\":\"VALID\",\"value\":\"test\"}";
        Value doubleValue = new StringValue("test");
        long timestamp = 1582722316;
        Flag flag = Flag.VALID;
        Record record = new Record(doubleValue, timestamp, flag);

        byte[] serializedRecord = parserService.serialize(new LoggingRecord("test", record));
        String serializedJson = new String(serializedRecord);
        assertEquals(controlString, serializedJson);
    }

    @Test
    void serializeByteArrayValue() throws SerializationException {
        String controlString = "{\"timestamp\":1582722316,\"flag\":\"VALID\",\"value\":\"dGVzdA==\"}";
        Value byteArrayValue = new ByteArrayValue("test".getBytes());
        long timestamp = 1582722316;
        Flag flag = Flag.VALID;
        Record record = new Record(byteArrayValue, timestamp, flag);

        byte[] serializedRecord = parserService.serialize(new LoggingRecord("test", record));
        String serializedJson = new String(serializedRecord);
        assertEquals(controlString, serializedJson);
    }

    @Test
    void deserializeTestDoubleValue() {
        String inputString = "{\"timestamp\":1582722316,\"flag\":\"VALID\",\"value\":3.0}";

        Record recordDes = parserService.deserialize(inputString.getBytes(), ValueType.DOUBLE);
        assertEquals(3.0, recordDes.getValue().asDouble());
    }

    @Test
    void deserializeByteArrayValue() {
        String inputString = "{\"timestamp\":1582722316,\"flag\":\"VALID\",\"value\":\"dGVzdA==\"}";

        Record recordDes = parserService.deserialize(inputString.getBytes(), ValueType.BYTE_ARRAY);
        assertEquals("test", new String(recordDes.getValue().asByteArray()));
    }

    @Test
    void deserializeTimestamp() {
        String inputString = "{\"timestamp\":1582722316,\"flag\":\"VALID\",\"value\":3.0}";

        Record recordDes = parserService.deserialize(inputString.getBytes(), ValueType.DOUBLE);
        assertEquals(1582722316, recordDes.getTimestamp().longValue());
    }

    @Test
    void deserializeFlag() {
        String inputString = "{\"timestamp\":1582722316,\"flag\":\"VALID\",\"value\":3.0}";

        Record recordDes = parserService.deserialize(inputString.getBytes(), ValueType.DOUBLE);
        assertEquals("VALID", recordDes.getFlag().name());
    }

    @Test
    void serialisationAndDeserialisationAreThreadSafe() {
        // this is pretty hard to test (at least I (dwerner) could not figure out how to in 1h, so I'm giving up now)
        // the methods should be thread safe if:
        // 1. there are no members in the class (making the methods inherited by ReactParser effectively static and thus
        // thread safe)
        // 2. the inherited methods have the 'synchronized' keyword -> looking for all public methods here, just to be
        // safe

        Set<Field> members = Arrays.stream(OpenmucParserServiceImpl.class.getDeclaredFields())
                .filter(f -> !Modifier.isStatic(f.getModifiers()))
                .collect(Collectors.toSet());
        if (members.isEmpty()) {
            System.out.println("OpenmucParserServiceImpl does not have non-static members and should be thread safe");
            return;
        }
        else {
            Set<Method> publicMethods = Arrays.stream(OpenmucParserServiceImpl.class.getDeclaredMethods())
                    .filter(m -> Modifier.isPublic(m.getModifiers()))
                    .collect(Collectors.toSet());
            for (Method method : publicMethods) {
                assertTrue(Modifier.isSynchronized(method.getModifiers()),
                        "Method '" + method + "' should have the 'synchronized' keyword");
            }
        }
    }
}
