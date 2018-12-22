/*
 * Copyright 2011-18 Fraunhofer ISE
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
package org.openmuc.framework.server.restws.test;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.data.ValueType;
import org.openmuc.framework.lib.json.Const;
import org.openmuc.framework.lib.json.FromJson;

public class TestJsonHelper_fromJson {

    private static String stringValueWithTicks = "\"" + Constants.STRING_VALUE + "\"";

    private static String[] sTestJsonValueArray;
    private static String sTestRecord;

    @BeforeClass
    public static void setup() {

        String testJsonDoubleValue = "\"value\":" + Constants.DOUBLE_VALUE;
        String testJsonFloatValue = "\"value\":" + Constants.FLOAT_VALUE;
        String testJsonLongValue = "\"value\":" + Constants.LONG_VALUE;
        String testJsonIntegerValue = "\"value\":" + Constants.INTEGER_VALUE;
        String testJsonShortValue = "\"value\":" + Constants.SHORT_VALUE;
        String testJsonByteValue = "\"value\":" + Constants.BYTE_VALUE;
        String testJsonBooleanValue = "\"value\":" + Constants.BOOLEAN_VALUE;
        String testJsonByteArrayValue = "\"value\":" + Arrays.toString(Constants.BYTE_ARRAY_VALUE);
        String testJsonStringValue = "\"value\":" + stringValueWithTicks;

        // ValueType enum: DOUBLE, FLOAT, LONG, INTEGER, SHORT, BYTE, BOOLEAN, BYTE_ARRAY, STRING
        String[] testJsonValueArray = { testJsonDoubleValue, testJsonFloatValue, testJsonLongValue,
                testJsonIntegerValue, testJsonShortValue, testJsonByteValue, testJsonBooleanValue,
                testJsonByteArrayValue, testJsonStringValue };

        String testRecord = "\"" + Const.RECORD + "\":{\"timestamp\":" + Constants.TIMESTAMP + ",\"flag\":\""
                + Constants.TEST_FLAG.toString() + "\",";
        sTestRecord = testRecord;
        sTestJsonValueArray = testJsonValueArray;
    }

    @Test
    public void test_jsonToRecord() {

        boolean result = true;
        String testMethodName = "Test_jsonToRecord";

        Set<ValueType> elements = EnumSet.allOf(ValueType.class);
        Iterator<ValueType> it = elements.iterator();
        Record record;
        ValueType valueType;
        int i = 0;

        while (it.hasNext()) {

            // build json record
            valueType = it.next();
            String jsonString = "{" + sTestRecord + sTestJsonValueArray[i] + Constants.JSON_OBJECT_END + '}';
            System.out.println(testMethodName + "; ValueType: " + valueType.toString() + "; JsonString: " + jsonString);
            FromJson json = new FromJson(jsonString);
            record = json.getRecord(valueType);

            // test JsonHelper response
            if (record.getTimestamp() != Constants.TIMESTAMP) {
                result = false;
                System.out
                        .println(testMethodName + ": result is \"" + result + "\"; error: Record timestamp is wrong.");
                break;
            }
            if (record.getFlag().compareTo(Constants.TEST_FLAG) != 0) {
                result = false;
                System.out.println(
                        testMethodName + ": result is \"" + result + "\"; error: Record flag is wrong. Should be "
                                + Constants.TEST_FLAG + " but is " + record.getFlag());
                break;
            }
            result = TestTools.testValue(testMethodName, valueType, record.getValue());
            ++i;
        }
        if (result) {
            System.out.println(testMethodName + ": result is " + result);
        }
        assertTrue(result);
    }

}
