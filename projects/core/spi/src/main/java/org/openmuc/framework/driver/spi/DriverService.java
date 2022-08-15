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

package org.openmuc.framework.driver.spi;

import org.openmuc.framework.config.ArgumentSyntaxException;
import org.openmuc.framework.config.DriverInfo;
import org.openmuc.framework.config.ScanException;
import org.openmuc.framework.config.ScanInterruptedException;

/**
 * 
 * The <code>DriverService</code> is the interface that all OpenMUC communication drivers have to implement and register
 * as a service in the OSGi environment. The OpenMUC Core Data Manager tracks this service and will therefore be
 * automatically notified of any new drivers in the framework. If sampling, listening or logging has been configured for
 * this driver, then the data manager will start right away with the appropriate actions.
 * 
 * A driver often implements a communication protocol but could also get its data from any other source (e.g. a file).
 * 
 * Some guidelines should be followed when implementing a driver for OpenMUC:
 * <ul>
 * <li>Logging may only be done with level <code>debug</code> or <code>trace</code>. Even these debug messages should be
 * used very sparsely. Only use them to further explain errors that occur.</li>
 * <li>If the connection to a device is interrupted throw a <code>ConnectionException</code>. The framework will then
 * try to reconnect by calling the <code>connect</code> function.</li>
 * <li>All unchecked exceptions thrown by the <code>DriverService</code> are caught by the OpenMUC framework and logged
 * with level <code>error</code>. It is bad practice to throw these unchecked exceptions because they can clutter the
 * log file and slow down performance if the function is called many times. Instead the appropriate Flag should be
 * returned for the affected channels.</li>
 * </ul>
 * 
 */
public interface DriverService {

    /**
     * Returns the driver information. Contains the driver's ID, a description of the driver and the syntax of various
     * configuration options.
     * 
     * @return the driver information
     */
    DriverInfo getInfo();

    /**
     * Scans for available devices. Once a device is found it is reported as soon as possible to the DeviceScanListener
     * through the <code>deviceFound()</code> function. Optionally this method may occasionally call the
     * updateScanProgress function of DeviceScanListener. The updateScanProgress function should pass the progress in
     * percent. The progress should never be explicitly set to 100%. The caller of this function will know that the
     * progress is at 100% once the function has returned.
     * 
     * @param settings
     *            scanning settings (e.g. location where to scan, baud rate etc.). The syntax is driver specific.
     * @param listener
     *            the listener that is notified of devices found and progress updates.
     * @throws UnsupportedOperationException
     *             if the method is not implemented by the driver
     * @throws ArgumentSyntaxException
     *             if an the settings string cannot be understood by the driver
     * @throws ScanException
     *             if an error occurs while scanning
     * @throws ScanInterruptedException
     *             if the scan was interrupted through a call of <code>interruptDeviceScan()</code> before it was done.
     */
    void scanForDevices(String settings, DriverDeviceScanListener listener)
            throws UnsupportedOperationException, ArgumentSyntaxException, ScanException, ScanInterruptedException;

    /**
     * A call of this function signals the driver to stop the device scan as soon as possible. The function should
     * return immediately instead of waiting until the device scan has effectively been interrupted. The function
     * scanForDevices() is to throw a ScanInterruptedException once the scan was really stopped. Note that there is no
     * guarantee that scanForDevices will throw the ScanInterruptedException as a result of this function call because
     * it could stop earlier for some other reason (e.g. successful finish, Exception etc.) instead.
     * 
     * @throws UnsupportedOperationException
     *             if the method is not implemented by the driver
     */
    void interruptDeviceScan() throws UnsupportedOperationException;

    /**
     * Attempts to connect to the given communication device using the given settings. The resulting connection shall be
     * returned as an object that implements the {@link Connection} interface. The framework will then call read/write
     * etc functions on the returned connection object. If the syntax of the given deviceAddresse, or settings String is
     * incorrect it will throw an <code>ArgumentSyntaxException</code>. If the connection attempt fails it throws a
     * <code>ConnectionException</code>.
     * 
     * Some communication protocols are not connection oriented. That means no connection has to be build up in order to
     * read or write data. In this case the connect function may optionally test if the device is reachable.
     * 
     * @param deviceAddress
     *            the configured device address.
     * @param settings
     *            the settings that should be used for the communication with this device.
     * @return the connection object that will be used for subsequent read/listen/write/scanForChannels/disconnect
     *         function calls.
     * @throws ArgumentSyntaxException
     *             if the syntax of the deviceAddress or settings string is incorrect.
     * @throws ConnectionException
     *             if the connection attempt fails.
     */
    Connection connect(String deviceAddress, String settings) throws ArgumentSyntaxException, ConnectionException;

}
