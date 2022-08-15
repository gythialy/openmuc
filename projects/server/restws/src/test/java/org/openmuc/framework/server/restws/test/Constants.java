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
package org.openmuc.framework.server.restws.test;

import org.openmuc.framework.data.Flag;

public final class Constants {

    public static final double DOUBLE_VALUE = 3.2415;
    public static final float FLOAT_VALUE = 3.2415f;
    public static final int INTEGER_VALUE = 10513;
    public static final int LONG_VALUE = 12345678;
    public static final short SHORT_VALUE = 1234;
    public static final byte BYTE_VALUE = 123;
    public static final boolean BOOLEAN_VALUE = true;
    public static final byte[] BYTE_ARRAY_VALUE = { 0, 1, 9, 10, 15, 16, 17, 127, -127, -81, -16, -1 };
    public static final String STRING_VALUE = "TestString";
    public static final Flag TEST_FLAG = Flag.VALID;
    public static final long TIMESTAMP = 1417783028138l;
    public static final String JSON_OBJECT_BEGIN = "{";
    public static final String JSON_OBJECT_END = "}";
}
