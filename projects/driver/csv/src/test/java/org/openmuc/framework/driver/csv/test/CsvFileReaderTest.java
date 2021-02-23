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
package org.openmuc.framework.driver.csv.test;

import org.junit.Assert;
import org.junit.Test;
import org.openmuc.framework.driver.csv.CsvFileReader;
import org.openmuc.framework.driver.spi.ConnectionException;

public class CsvFileReaderTest {

    String dir = System.getProperty("user.dir");

    @Test
    public void test() {

        String fileName = dir + "/src/test/resources/test_data.csv";

        try {
            CsvFileReader.readCsvFile(fileName);
            Assert.assertTrue(true);
        } catch (ConnectionException e) {
            e.printStackTrace();
            Assert.assertTrue(false);
        }

    }

}
