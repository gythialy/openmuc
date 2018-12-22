package org.openmuc.framework.core.datamanager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.openmuc.framework.core.datamanager.ChannelConfigImpl.timeStringToMillis;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.openmuc.framework.config.ParseException;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;

@RunWith(JUnitParamsRunner.class)
public class ChannelConfigImplTest {

    @Test
    @Parameters({ "99ms, 99", "100, 100", "1s, 1000", "1m, 60000", "0h, 0", "5h, 18000000", "24h, 86400000" })
    public void testTimeStringToMillis(String timeStr, Integer expTimeInMillis) throws Exception {
        Integer millis = timeStringToMillis(timeStr);
        assertEquals(expTimeInMillis, millis);
    }

    @Test
    public void testEmptyTimeStringToMillis() throws Exception {
        Integer millis = timeStringToMillis("");
        assertNull(millis);
    }

    @Test(expected = ParseException.class)
    @Parameters({ "99w", "1y", "a77" })
    public void testTimeStringToMillisFail(String timeStr) throws Exception {
        timeStringToMillis(timeStr);
    }

    @Test
    @Parameters({ "99ms, 99", "5ms, 5", "100ms, 100", "1s, 1000", "59s, 59000", "59001ms,59001", "1m, 60000", "0, 0",
            "5h, 18000000", "24h, 86400000" })
    public void testTimeToString(String expectedTimeStr, int millis) throws Exception {
        String resTime = ChannelConfigImpl.millisToTimeString(millis);
        assertEquals(expectedTimeStr, resTime);
    }
}
