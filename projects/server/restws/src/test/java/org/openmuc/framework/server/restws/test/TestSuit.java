package org.openmuc.framework.server.restws.test;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({TestJsonHelper_fromJson.class /* , TestJsonHelper_toJson.class, /*<weitereKlassen.class> */})
public class TestSuit {

    @BeforeClass
    public static void setUp() {
        System.out.println("setting up");
    }

    @AfterClass
    public static void tearDown() {
        System.out.println("tearing down");
    }
}
