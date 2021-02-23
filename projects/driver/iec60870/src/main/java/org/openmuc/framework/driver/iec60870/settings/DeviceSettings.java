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
package org.openmuc.framework.driver.iec60870.settings;

import org.openmuc.framework.config.ArgumentSyntaxException;

public class DeviceSettings extends GenericSetting {

    protected int message_fragment_timeout = -1;
    protected int cot_field_length = -1;
    protected int common_address_field_length = -1;
    protected int ioa_field_length = -1;
    protected int max_time_no_ack_received = -1;
    protected int max_time_no_ack_sent = -1;
    protected int max_idle_time = -1;
    protected int max_unconfirmed_ipdus_received = -1;
    protected int stardt_con_timeout = 5000;
    protected int read_timeout = 5000;

    public static enum Option implements OptionI {
        MESSAGE_FRAGMENT_TIMEOUT("mft", Integer.class, false),
        COT_FIELD_LENGTH("cfl", Integer.class, false),
        COMMON_ADDRESS_FIELD_LENGTH("cafl", Integer.class, false),
        IOA_FIELD_LENGTH("ifl", Integer.class, false),
        MAX_TIME_NO_ACK_RECEIVED("mtnar", Integer.class, false),
        MAX_TIME_NO_ACK_SENT("mtnas", Integer.class, false),
        MAX_IDLE_TIME("mit", Integer.class, false),
        MAX_UNCONFIRMED_IPDUS_RECEIVED("mupr", Integer.class, false),
        STARDT_CON_TIMEOUT("sct", Integer.class, false),
        READ_TIMEOUT("rt", Integer.class, false);

        private String prefix;
        private Class<?> type;
        private boolean mandatory;

        private Option(String prefix, Class<?> type, boolean mandatory) {
            this.prefix = prefix;
            this.type = type;
            this.mandatory = mandatory;
        }

        @Override
        public String prefix() {
            return this.prefix;
        }

        @Override
        public Class<?> type() {
            return this.type;
        }

        @Override
        public boolean mandatory() {
            return this.mandatory;
        }
    }

    protected Class<? extends Enum<?>> option() {
        return Option.class;
    }

    public DeviceSettings(String settings) throws ArgumentSyntaxException {
        parseFields(settings, Option.class);
    }

    public int messageFragmentTimeout() {
        return message_fragment_timeout;
    }

    public int cotFieldLength() {
        return cot_field_length;
    }

    public int commonAddressFieldLength() {
        return common_address_field_length;
    }

    public int ioaFieldLength() {
        return ioa_field_length;
    }

    public int maxTimeNoAckReceived() {
        return max_time_no_ack_received;
    }

    public int maxTimeNoAckSent() {
        return max_time_no_ack_sent;
    }

    public int maxIdleTime() {
        return max_idle_time;
    }

    public int maxUnconfirmedIPdusReceived() {
        return max_unconfirmed_ipdus_received;
    }

    public int stardtConTimeout() {
        return stardt_con_timeout;
    }

    /**
     * Optional: read/sampling timeout in milliseconds<br>
     * Default timeout is 5000 milliseconds
     * 
     * @return read timeout in milliseconds
     */
    public int readTimeout() {
        return read_timeout;
    }

}
