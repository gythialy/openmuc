package org.openmuc.framework.server.restws.test;

import junit.framework.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.data.Value;
import org.openmuc.framework.data.ValueType;
import org.openmuc.framework.server.restws.JsonHelper;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Set;

import static org.junit.Assert.assertTrue;

@SuppressWarnings("deprecation")
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
        String[] testJsonValueArray = {testJsonDoubleValue, testJsonFloatValue, testJsonLongValue, testJsonIntegerValue,
                testJsonShortValue, testJsonByteValue, testJsonBooleanValue, testJsonByteArrayValue, testJsonStringValue};

        String testRecord = "{\"timestamp\":" + Constants.TIMESTAMP + ",\"flag\":\"" + Constants.TEST_FLAG.toString() + "\",";
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
            String jsonString = sTestRecord + sTestJsonValueArray[i] + Constants.JSON_OBJECT_END;
            System.out.println(testMethodName + "; ValueType: " + valueType.toString() + "; JsonString: " + jsonString);
            record = JsonHelper.jsonToRecord(valueType, jsonString);

            // test JsonHelper response
            if (record.getTimestamp() != Constants.TIMESTAMP) {
                result = false;
                System.out.println(testMethodName + ": result is \"" + result + "\"; error: Record timestamp is wrong.");
                break;
            }
            if (record.getFlag().compareTo(Constants.TEST_FLAG) != 0) {
                result = false;
                System.out.println(
                        testMethodName + ": result is \"" + result + "\"; error: Record flag is wrong. Should be " + Constants.TEST_FLAG
                                + " but is " + record
                                .getFlag());
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

    @Test
    public void test_jsonToValue() {

        boolean result = true;
        String testMethodName = "Test_jsonToValue";

        Set<ValueType> elements = EnumSet.allOf(ValueType.class);
        Iterator<ValueType> it = elements.iterator();
        ValueType valueType;
        int i = 0;
        Value value;

        while (it.hasNext()) {

            // build json value
            valueType = it.next();
            String jsonString = Constants.JSON_OBJECT_BEGIN + sTestJsonValueArray[i] + Constants.JSON_OBJECT_END;
            System.out.println(testMethodName + "; ValueType: " + valueType.toString() + "; JsonString: " + jsonString);
            value = JsonHelper.jsonToValue(valueType, jsonString);

            // test JsonHelper response
            result = TestTools.testValue(testMethodName, valueType, value);
            ++i;
        }
        if (result) {
            System.out.println(testMethodName + ": result is " + result);
        }
        assertTrue(result);
    }

    @Test
    public void test_jsonToConfigValue() {

        boolean result = true;
        String testMethodName = "Test_jsonToConfigValue";

        // build json ConfigValue
        String jsonString = Constants.JSON_OBJECT_BEGIN + sTestJsonValueArray[sTestJsonValueArray.length - 1] + Constants.JSON_OBJECT_END;
        System.out.println(testMethodName + "; ValueType: " + ValueType.STRING + "; JsonString: " + jsonString);
        String response = JsonHelper.jsonToConfigValue(jsonString);

        // test JsonHelper response
        Assert.assertEquals(testMethodName + ": Expected String is not equal the actual", Constants.STRING_VALUE, response);
        if (result) {
            System.out.println(testMethodName + ": result is " + result);
        }
        assertTrue(result);
    }

}
