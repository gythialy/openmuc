
package org.openmuc.framework.data;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;

@RunWith(JUnitParamsRunner.class)
public class StringValueTest {
    @Test
    @Parameters(method = "params")
    public void testBooleanConvert(String value, boolean expected) throws Exception {
        assertEquals(expected, new StringValue(value).asBoolean());
    }

    public Object params() {
        return new Object[][] { { "false", false }, { "true", true }, { "jhbvce", false }, { "TRUE", true },
                { "TRuE", true } };
    }

    @Test(expected = TypeConversionException.class)
    public void testException() throws Exception {
        new StringValue("98394kdbk").asDouble();
    }

}
