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

package org.openmuc.framework.dataaccess;

import org.openmuc.framework.data.Flag;
import org.openmuc.framework.data.Value;

/**
 * A container to write a to channel.
 * 
 * @see Channel#getWriteContainer()
 * @see DataAccessService#write(java.util.List)
 */
public interface WriteValueContainer {

    /**
     * Set the value of the container.
     * 
     * @param value
     *            the value to set on the channel.
     * @see #getChannel()
     */
    void setValue(Value value);

    /**
     * Get the value of the container.
     * 
     * @return the value which has been set via {@link #setValue(Value)}.
     */
    Value getValue();

    /**
     * Get the resulting of the write action.
     * 
     * @return the result of the write. Only available if the value has been written.
     */
    Flag getFlag();

    /**
     * Get the corresponding channel.
     * 
     * @return the channel.
     */
    Channel getChannel();

}
