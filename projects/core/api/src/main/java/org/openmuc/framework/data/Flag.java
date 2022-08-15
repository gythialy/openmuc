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

package org.openmuc.framework.data;

import java.util.HashMap;
import java.util.Map;

/**
 * Flags indicates the state of a record. It identifies which type of error occurred or if the value is valid.<br>
 * More informations of the occurred errors can often seen in the OpenMUC log files in the "log" folder.
 */
public enum Flag {

    /**
     * Flag 1: Valid data.
     */
    VALID(1),
    /**
     * Flag 2: Timeout occurred in a DataManager task. For example, if the sampling task took longer then the specified
     * samplingTimeout then this flag is set.
     */
    TIMEOUT(2),
    /**
     * Flag 3: If the error could not be specified.
     */
    UNKNOWN_ERROR(3),
    /**
     * Flag 5: The device was still busy while sampling. This case can happen if the previous sampling failed due to a
     * timeout (Flag 15). The sampling task couldn't start because an other sampling task is still running.
     */
    DEVICE_OR_INTERFACE_BUSY(5),
    /**
     * Flag 6: If a access method is chosen which is not supported or implemented.
     */
    ACCESS_METHOD_NOT_SUPPORTED(6),
    /**
     * Flag 7: This flag is mostly shown in listening mode and no value are received.
     */
    NO_VALUE_RECEIVED_YET(7),
    /**
     * Flag 8: The driver tries to connect to the device.
     */
    CONNECTING(8),
    /**
     * Flag 9: The driver was not able to connect and retries to connect after the configured time.
     */
    WAITING_FOR_CONNECTION_RETRY(9),
    /**
     * Flag 10: The driver is disconnecting.
     */
    DISCONNECTING(10),
    /**
     * Flag 11: The driver with the configured driver id could not found. Possible reasons:<br>
     * - the driver bundle is missing<br>
     * - the configured driver id is wrong
     */
    DRIVER_UNAVAILABLE(11),
    /**
     * Flag 12: Neither sampling nor listening are activated
     */
    SAMPLING_AND_LISTENING_DISABLED(12),
    /**
     * Flag 13: The channel is disabled.
     */
    DISABLED(13),
    /**
     * Flag 14: The channel was deleted.
     */
    CHANNEL_DELETED(14),
    /**
     * Flag 15: Started sampling task too late and timed out due to a timeout of the previous sampling task.
     */
    STARTED_LATE_AND_TIMED_OUT(15),
    /**
     * Flag 16: The driver was not able to identify the error.
     */
    DRIVER_THREW_UNKNOWN_EXCEPTION(16),
    /**
     * Flag 17: The communication device is not connected.
     */
    COMM_DEVICE_NOT_CONNECTED(17),
    /**
     * Flag 18: The configured channel address does not fit to the driver channel address syntax.
     */
    DRIVER_ERROR_CHANNEL_ADDRESS_SYNTAX_INVALID(18),
    /**
     * Flag 19: The driver was not able to find a channel with the configured name.
     */
    DRIVER_ERROR_CHANNEL_WITH_THIS_ADDRESS_NOT_FOUND(19),
    /**
     * Flag 20: The channel is not accessible.
     */
    DRIVER_ERROR_CHANNEL_NOT_ACCESSIBLE(20),
    /**
     * Flag 21: The channel is temporarily not accessible.
     */
    DRIVER_ERROR_CHANNEL_TEMPORARILY_NOT_ACCESSIBLE(21),
    /**
     * Flag 22: The protocol value type is not convertible to this OpenMUC value type.
     */
    DRIVER_ERROR_CHANNEL_VALUE_TYPE_CONVERSION_EXCEPTION(22),
    /**
     * Flag 23: Infeasible to sample this group in one request. Perhaps the group is to big.
     */
    INFEASIBLE_TO_SAMPLE_CHANNEL_GROUP_IN_ONE_REQUEST(23),
    /**
     * Flag 24: The driver could not find the group with the configured name.
     */
    DRIVER_ERROR_SAMPLING_GROUP_NOT_FOUND(24),
    /**
     * Flag 25: The group is not accessible
     */
    DRIVER_ERROR_SAMPLING_GROUP_NOT_ACCESSIBLE(25),
    /**
     * Flag 26: The channel is not part of the group.
     */
    DRIVER_ERROR_CHANNEL_NOT_PART_OF_SAMPLING_GROUP(26),
    /**
     * Flag 27: It is not allowed to write null values.
     */
    CANNOT_WRITE_NULL_VALUE(27),
    /**
     * Flag 28: Error while reading.
     */
    DRIVER_ERROR_READ_FAILURE(28),
    /**
     * Flag 29: Connection exception. Connection is now disconnected or temporarily disconnected.
     */
    CONNECTION_EXCEPTION(29),
    /**
     * Flag 30: Timeout occurred in driver. Try to increase device specific timeout in deviceSettings.
     */
    DRIVER_ERROR_TIMEOUT(30),
    /**
     * Flag 31: The driver was not able to decoding the received response.
     */
    DRIVER_ERROR_DECODING_RESPONSE_FAILED(31),
    /**
     * Flag 32: Data logging is not activated in the configuration.
     */
    DATA_LOGGING_NOT_ACTIVE(32),
    /**
     * Flag 33: The driver error is not specified.
     */
    DRIVER_ERROR_UNSPECIFIED(33),
    /**
     * Flag 34: Got a "not a number" value.
     */
    VALUE_IS_NAN(34),
    /**
     * Flag 35: Got a "infinity" value.
     */
    VALUE_IS_INFINITY(35),
    /**
     * Flag 50: Error flags for custom record states.
     */
    CUSTOM_ERROR_0(50),
    /**
     * Flag 51: Error flags for custom record states.
     */
    CUSTOM_ERROR_1(51),
    /**
     * Flag 52: Error flags for custom record states.
     */
    CUSTOM_ERROR_2(52),
    /**
     * Flag 53: Error flags for custom record states.
     */
    CUSTOM_ERROR_3(53),
    /**
     * Flag 54: Error flags for custom record states.
     */
    CUSTOM_ERROR_4(54),
    /**
     * Flag 55: Error flags for custom record states.
     */
    CUSTOM_ERROR_5(55),
    /**
     * Flag 56: Error flags for custom record states.
     */
    CUSTOM_ERROR_6(56),
    /**
     * Flag 57: Error flags for custom record states.
     */
    CUSTOM_ERROR_7(57),
    /**
     * Flag 58: Error flags for custom record states.
     */
    CUSTOM_ERROR_8(58),
    /**
     * Flag 59: Error flags for custom record states.
     */
    CUSTOM_ERROR_9(59);

    private final int code;

    private Flag(int code) {
        this.code = code;
    }

    public byte getCode() {
        return (byte) code;
    }

    private static final Map<Byte, Flag> idMap = new HashMap<>();

    static {
        for (Flag enumInstance : Flag.values()) {
            if (idMap.put(enumInstance.getCode(), enumInstance) != null) {
                throw new IllegalArgumentException("duplicate ID: " + enumInstance.getCode());
            }
        }
    }

    public static Flag newFlag(int code) {
        Flag enumInstance = idMap.get((byte) code);
        if (enumInstance == null) {
            throw new IllegalArgumentException("Unknown Flag code: " + code);
        }
        return enumInstance;
    }

}
