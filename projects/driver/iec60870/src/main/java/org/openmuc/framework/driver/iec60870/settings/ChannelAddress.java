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
package org.openmuc.framework.driver.iec60870.settings;

import org.openmuc.framework.config.ArgumentSyntaxException;

public class ChannelAddress extends GenericSetting {

    protected int common_address = 1;
    protected int type_id = 0;
    protected int ioa = 0;
    protected String data_type = "v";
    protected int index = -1;
    protected int multiple = 1;
    // protected String command = "";
    protected boolean select = false;

    protected static enum Option implements OptionI {
        COMMON_ADDRESS("ca", Integer.class, true),
        TYPE_ID("t", Integer.class, true),
        IOA("ioa", Integer.class, true),
        DATA_TYPE("dt", String.class, false),
        INDEX("i", Integer.class, false),
        MULTIPLE("m", Integer.class, false),
        // COMMAND("c", String.class, false),
        SELECT("s", Boolean.class, false);

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

    public ChannelAddress(String channelAddress) throws ArgumentSyntaxException {
        parseFields(channelAddress, Option.class);
    }

    /**
     * Type Identification
     * 
     * @return type id as integer
     */
    public int typeId() {
        return type_id;
    }

    /**
     * Information Object Address
     *
     * @return IOA as integer
     */
    public int ioa() {
        return ioa;
    }

    /**
     * The common address of device
     * 
     * @return the comman address as integer
     */
    public int commonAddress() {
        return common_address;
    }

    /**
     * Meanings if boolean is TRUE<br>
     * v (value) / ts (timestamp) / iv (in/valid) / nt (not topical) / sb (substituted) / bl (blocked) / ov (overflow) /
     * ei (elapsed time invalid) / ca (counter was adjusted since last reading) / cy (counter overflow occurred in the
     * corresponding integration period)
     * 
     * @return the data type as string
     */
    public String dataType() {
        return data_type;
    }

    /**
     * Optional: only needed if VARIABLE STRUCTURE QUALIFIER of APSDU is 1
     * 
     * @return the index as integer
     */
    public int index() {
        return index;
    }

    /**
     * Optional: Take multiple IOAs or indices to one value. Only few data types e.g. Binary Types.
     * 
     * @return the multiple value as integer
     */
    public int multiple() {
        return multiple;
    }

    // /**
    // * Optional: command type
    // *
    // * @return the command type
    // */
    // public String command() {
    // return command;
    // }

    /**
     * Optional: Qualifier execute/select<br>
     * Default id false for execute
     * 
     * @return true for select and false for execute
     */
    public boolean select() {
        return select;
    }

}
