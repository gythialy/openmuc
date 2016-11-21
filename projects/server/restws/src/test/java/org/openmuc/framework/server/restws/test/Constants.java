/*
 * Copyright 2011-16 Fraunhofer ISE
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

import org.openmuc.framework.data.Flag;

public final class Constants {

    public final static double DOUBLE_VALUE = 3.2415;
    public final static float FLOAT_VALUE = 3.2415f;
    public final static int INTEGER_VALUE = 10513;
    public final static int LONG_VALUE = 12345678;
    public final static short SHORT_VALUE = 1234;
    public final static byte BYTE_VALUE = 123;
    public final static boolean BOOLEAN_VALUE = true;
    public final static byte[] BYTE_ARRAY_VALUE = { 0, 1, 9, 10, 15, 16, 17, 127, -127, -81, -16, -1 };
    public final static String STRING_VALUE = "TestString";
    public final static Flag TEST_FLAG = Flag.VALID;
    public final static long TIMESTAMP = 1417783028138l;
    public final static String JSON_OBJECT_BEGIN = "{";
    public final static String JSON_OBJECT_END = "}";
}
