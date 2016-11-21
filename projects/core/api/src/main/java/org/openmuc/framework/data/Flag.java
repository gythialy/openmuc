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

package org.openmuc.framework.data;

public enum Flag {

    VALID(1),
    TIMEOUT(2),
    UNKNOWN_ERROR(3),
    DEVICE_OR_INTERFACE_BUSY(5),
    ACCESS_METHOD_NOT_SUPPORTED(6),
    NO_VALUE_RECEIVED_YET(7),
    CONNECTING(8),
    WAITING_FOR_CONNECTION_RETRY(9),
    DISCONNECTING(10),
    DRIVER_UNAVAILABLE(11),
    SAMPLING_AND_LISTENING_DISABLED(12),
    DISABLED(13),
    CHANNEL_DELETED(14),
    STARTED_LATE_AND_TIMED_OUT(15),
    DRIVER_THREW_UNKNOWN_EXCEPTION(16),
    COMM_DEVICE_NOT_CONNECTED(17),
    DRIVER_ERROR_CHANNEL_ADDRESS_SYNTAX_INVALID(18),
    DRIVER_ERROR_CHANNEL_WITH_THIS_ADDRESS_NOT_FOUND(19),
    DRIVER_ERROR_CHANNEL_NOT_ACCESSIBLE(20),
    DRIVER_ERROR_CHANNEL_TEMPORARILY_NOT_ACCESSIBLE(21),
    DRIVER_ERROR_CHANNEL_VALUE_TYPE_CONVERSION_EXCEPTION(22),
    INFEASIBLE_TO_SAMPLE_CHANNEL_GROUP_IN_ONE_REQUEST(23),
    DRIVER_ERROR_SAMPLING_GROUP_NOT_FOUND(24),
    DRIVER_ERROR_SAMPLING_GROUP_NOT_ACCESSIBLE(25),
    DRIVER_ERROR_CHANNEL_NOT_PART_OF_SAMPLING_GROUP(26),
    CANNOT_WRITE_NULL_VALUE(27),
    DRIVER_ERROR_READ_FAILURE(28),
    CONNECTION_EXCEPTION(29),
    DRIVER_ERROR_TIMEOUT(30),
    DRIVER_ERROR_DECODING_RESPONSE_FAILED(31),
    DATA_LOGGING_NOT_ACTIVE(32),
    DRIVER_ERROR_UNSPECIFIED(33),
    VALUE_IS_NAN(34),
    VALUE_IS_INFINITY(35),
    CUSTOM_ERROR_0(50),
    CUSTOM_ERROR_1(51),
    CUSTOM_ERROR_2(52),
    CUSTOM_ERROR_3(53),
    CUSTOM_ERROR_4(54),
    CUSTOM_ERROR_5(55),
    CUSTOM_ERROR_6(56),
    CUSTOM_ERROR_7(57),
    CUSTOM_ERROR_8(58),
    CUSTOM_ERROR_9(59);

    private final int code;

    private Flag(int code) {
        this.code = code;
    }

    public byte getCode() {
        return (byte) code;
    }

    public static Flag newFlag(int code) {
        switch (code) {
        case 1:
            return Flag.VALID;
        case 2:
            return Flag.TIMEOUT;
        case 3:
            return Flag.UNKNOWN_ERROR;
        case 5:
            return Flag.DEVICE_OR_INTERFACE_BUSY;
        case 6:
            return Flag.ACCESS_METHOD_NOT_SUPPORTED;
        case 7:
            return Flag.NO_VALUE_RECEIVED_YET;
        case 8:
            return Flag.CONNECTING;
        case 9:
            return Flag.WAITING_FOR_CONNECTION_RETRY;
        case 10:
            return Flag.DISCONNECTING;
        case 11:
            return Flag.DRIVER_UNAVAILABLE;
        case 12:
            return Flag.SAMPLING_AND_LISTENING_DISABLED;
        case 13:
            return Flag.DISABLED;
        case 14:
            return Flag.CHANNEL_DELETED;
        case 15:
            return Flag.STARTED_LATE_AND_TIMED_OUT;
        case 16:
            return Flag.DRIVER_THREW_UNKNOWN_EXCEPTION;
        case 17:
            return Flag.COMM_DEVICE_NOT_CONNECTED;
        case 18:
            return Flag.DRIVER_ERROR_CHANNEL_ADDRESS_SYNTAX_INVALID;
        case 19:
            return Flag.DRIVER_ERROR_CHANNEL_WITH_THIS_ADDRESS_NOT_FOUND;
        case 20:
            return Flag.DRIVER_ERROR_CHANNEL_NOT_ACCESSIBLE;
        case 21:
            return Flag.DRIVER_ERROR_CHANNEL_TEMPORARILY_NOT_ACCESSIBLE;
        case 22:
            return Flag.DRIVER_ERROR_CHANNEL_VALUE_TYPE_CONVERSION_EXCEPTION;
        case 23:
            return Flag.INFEASIBLE_TO_SAMPLE_CHANNEL_GROUP_IN_ONE_REQUEST;
        case 24:
            return Flag.DRIVER_ERROR_SAMPLING_GROUP_NOT_FOUND;
        case 25:
            return Flag.DRIVER_ERROR_SAMPLING_GROUP_NOT_ACCESSIBLE;
        case 26:
            return Flag.DRIVER_ERROR_CHANNEL_NOT_PART_OF_SAMPLING_GROUP;
        case 27:
            return Flag.CANNOT_WRITE_NULL_VALUE;
        case 28:
            return Flag.DRIVER_ERROR_READ_FAILURE;
        case 29:
            return Flag.CONNECTION_EXCEPTION;
        case 30:
            return Flag.DRIVER_ERROR_TIMEOUT;
        case 31:
            return Flag.DRIVER_ERROR_DECODING_RESPONSE_FAILED;
        case 32:
            return Flag.DATA_LOGGING_NOT_ACTIVE;
        case 33:
            return Flag.DRIVER_ERROR_UNSPECIFIED;
        case 50:
            return Flag.CUSTOM_ERROR_0;
        case 51:
            return Flag.CUSTOM_ERROR_1;
        case 52:
            return Flag.CUSTOM_ERROR_2;
        case 53:
            return Flag.CUSTOM_ERROR_3;
        case 54:
            return Flag.CUSTOM_ERROR_4;
        case 55:
            return Flag.CUSTOM_ERROR_5;
        case 56:
            return Flag.CUSTOM_ERROR_6;
        case 57:
            return Flag.CUSTOM_ERROR_7;
        case 58:
            return Flag.CUSTOM_ERROR_8;
        case 59:
            return Flag.CUSTOM_ERROR_9;
        default:
            throw new IllegalArgumentException("Unknown Flag code: " + code);
        }
    }
}
