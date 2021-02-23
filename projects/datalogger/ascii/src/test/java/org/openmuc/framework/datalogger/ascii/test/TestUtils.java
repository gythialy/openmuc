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
package org.openmuc.framework.datalogger.ascii.test;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

import org.openmuc.framework.datalogger.ascii.utils.LoggerUtils;

public class TestUtils {
    public static final String TESTFOLDER = "test";
    public static final String TESTFOLDERPATH = System.getProperty("user.dir") + "/" + TESTFOLDER + "/";

    public static Calendar stringToDate(String format, String strDate) {

        SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.GERMAN);
        Date date = null;
        try {
            date = sdf.parse(strDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        Calendar calendar = new GregorianCalendar(Locale.getDefault());
        calendar.setTime(date);

        return calendar;
    }

    public static void deleteExistingFile(int loggingInterval, int loggingTimeOffset, Calendar calendar) {

        String filename = LoggerUtils.getFilename(loggingInterval, loggingTimeOffset, calendar.getTimeInMillis());
        File file = new File(TestUtils.TESTFOLDERPATH + filename);

        if (file.exists()) {
            System.out.println("Delete File " + filename);
            file.delete();
        }

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
