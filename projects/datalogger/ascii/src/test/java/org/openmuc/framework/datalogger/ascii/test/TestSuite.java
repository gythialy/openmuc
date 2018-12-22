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
package org.openmuc.framework.datalogger.ascii.test;

import java.io.File;
import java.io.FileNotFoundException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ LogFileReaderTestSingleFile.class, LogFileReaderTestBrokenFile.class,
        LogFileReaderTestMultipleFiles.class, LogFileWriterTest.class, MiscTests.class })
public class TestSuite {

    private static final String TESTFOLDER = "test";

    @BeforeClass
    public static void setUp() {

        System.out.println("setting up");
        createTestFolder();
    }

    @AfterClass
    public static void tearDown() {

        System.out.println("tearing down");
        deleteTestFolder();
    }

    public static void createTestFolder() {

        File testFolder = new File(TESTFOLDER);
        if (!testFolder.exists()) {
            testFolder.mkdir();
        }
    }

    public static void deleteTestFolder() {

        File testFolder = new File(TESTFOLDER);
        try {
            deleteRecursive(testFolder);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private static boolean deleteRecursive(File path) throws FileNotFoundException {

        if (!path.exists()) {
            System.out.println("Method deleteRecursive(): Path does not exists. " + path.getAbsolutePath());
        }
        boolean ret = true;
        if (path.isDirectory()) {
            for (File f : path.listFiles()) {
                ret = ret && deleteRecursive(f);
            }
        }
        return ret && path.delete();
    }

}
