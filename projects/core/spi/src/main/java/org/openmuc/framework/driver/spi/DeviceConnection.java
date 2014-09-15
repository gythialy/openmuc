/*
 * Copyright 2011-14 Fraunhofer ISE
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

package org.openmuc.framework.driver.spi;

/**
 * Interface containing the following connection parameters.
 * <ul>
 * <li><b>interfaceAddress</b> The address of the interface</li>
 * <li><b>deviceAddress</b> The address of the device (e.g. on the local bus / ip network)</li>
 * <li><b>settings</b> A settings String which may contain several connection parameters (e.g.: baud, parity, stopbits
 * for serial connections)</li>
 * <li><b>connectionHandle</b>The connection handle</li>
 * </ul>
 */
public interface DeviceConnection {

    public String getInterfaceAddress();

    public String getDeviceAddress();

    public String getSettings();

    public Object getConnectionHandle();

}
