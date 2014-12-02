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

import org.openmuc.framework.config.*;

import java.util.List;

/**
 * The <code>DriverService</code> is the interface that all OpenMUC communication drivers have to implement and register
 * as a service in the OSGi environment. The OpenMUC Core Data Manager tracks this service and will therefore be
 * automatically notified of any new drivers in the framework. If sampling, listening or logging has been configured for
 * this driver, then the data manager will start right away with the appropriate actions.
 * <p/>
 * The OpenMUC framework can give certain guarantees about the order of the functions it calls:
 * <ul>
 * <li>Communication related functions (e.g. connect,read,write..) are never called concurrently for the same device.</li>
 * <li>Communication related functions for different devices may be called concurrently by the framework unless an
 * interfaceAddress was configured. Thus drivers for communication protocols that do not support the parallel
 * communication to different communication devices over the same medium should make the configuration of an
 * interfaceAddress mandatory.</li>
 * <li>The framework calls read,listen,write or channelScan only if a the device is considered connected. The device is
 * only considered connected if the connect function has been called successfully.</li>
 * <li>Before a driver service is unregistered the framework calls disconnect for all connected devices. The disconnect
 * function should do any necessary resource clean up.</li>
 * </ul>
 * <p/>
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
 */
public interface DriverService {

    /**
     * Returns the driver information. Contains the driver's ID, a description of the driver and the syntax of various
     * configuration options.
     *
     * @return the driver information
     */
    public DriverInfo getInfo();

    /**
     * Scans for available devices. Once a device is found it is reported as soon as possible to the DeviceScanListener
     * through the <code>deviceFound()</code> function. Optionally this method may occasionally call the
     * updateScanProgress function of DeviceScanListener. The updateScanProgress function should be passed the progress
     * in percent. The progress should never be explicitly set to 100%. The caller of this function will know that the
     * progress is at 100% once the function has returned.
     *
     * @param settings scanning settings (e.g. location where to scan, baud rate etc.). The syntax is driver specific.
     * @param listener the listener that is notified of devices found and progress updates.
     * @throws UnsupportedOperationException if the method is not implemented by the driver
     * @throws ArgumentSyntaxException       if an the settings string cannot be understood by the driver
     * @throws ScanException                 if an error occurs while scanning
     * @throws ScanInterruptedException      if the scan was interrupted through a call of <code>interruptDeviceScan()</code> before it was done.
     */
    public void scanForDevices(String settings, DriverDeviceScanListener listener)
            throws UnsupportedOperationException, ArgumentSyntaxException, ScanException,
            ScanInterruptedException;

    /**
     * A call of this function signals the driver to stop the device scan as soon as possible. The function should
     * return immediately instead of waiting until the device scan has effectively been interrupted. The function
     * scanForDevices() is to throw a ScanInterruptedException once the scan was really stopped. Note that there is no
     * guarantee that scanForDevices will throw the ScanInterruptedException as a result of this function call because
     * it could stop earlier for some other reason (e.g. successful finish, Exception etc.) instead.
     *
     * @throws UnsupportedOperationException if the method is not implemented by the driver
     */
    public void interruptDeviceScan() throws UnsupportedOperationException;

    /**
     * Scan a given communication device for available data channels.
     *
     * @param connection the DeviceConnection of the communication device that is to be scanned for data channels.
     * @param settings   scanning settings. The syntax is driver specific.
     * @return A list of channels that were found.
     * @throws ArgumentSyntaxException       if an the settings string cannot be understood by the driver
     * @throws UnsupportedOperationException if the method is not implemented by the driver.
     * @throws ScanException                 if an error occurs while scanning but the connection is still alive.
     * @throws ConnectionException           if an error occurs while scanning and the connection was closed
     */
    public List<ChannelScanInfo> scanForChannels(DeviceConnection connection, String settings)
            throws UnsupportedOperationException, ArgumentSyntaxException, ScanException,
            ConnectionException;

    /**
     * Attempts to connect to the given communication device using the given settings. The resulting connection resource
     * object can be return by this function. The framework will then pass the connection object in all subsequent
     * function calls meant for this device. If the syntax of the given interfaceAddress, deviceAddresse, or settings
     * String is incorrect it will throw an <code>ArgumentSyntaxException</code>. If the connection attempt fails it
     * throws a <code>ConnectionException</code>.
     *
     * @param interfaceAddress the configured interface address. Will be <code>null</code> if not configured.
     * @param deviceAddress    the configured device address.
     * @param settings         the settings that should be used for the communication with this device.
     * @return the connection handle that is passed to subsequent read/listen/write/scanForChannels/disconnect function
     * calls.
     * @throws ArgumentSyntaxException if the syntax of interfaceAddress, deviceAddress or settings is incorrect.
     * @throws ConnectionException     if the connection attempt fails.
     */
    public Object connect(String interfaceAddress, String deviceAddress, String settings)
            throws ArgumentSyntaxException, ConnectionException;

    /**
     * Disconnects or closes the given connection. Cleans up any resources associated with the connection.
     *
     * @param connection the device connection that is to closed.
     */
    public void disconnect(DeviceConnection connection);

    /**
     * Reads the data channels that correspond to the given record containers. The read result is returned by setting
     * the record in the containers. If for some reason no value can be read the record should be set anyways. In this
     * case the record constructor that takes only a flag should be used. The flag shall best describe the reason of
     * failure. If no record is set the default Flag is <code>Flag.DRIVER_ERROR_CHANNEL_NOT_ACCESSIBLE</code>. If the
     * connection to the device is interrupted, then any necessary resources that correspond to this connection should
     * be cleaned up and a <code>ConnectionException</code> shall be thrown.
     *
     * @param connection          the device connection that is to be used to read the channels.
     * @param containers          the containers hold the information of what channels are to be read. They will be filled by this
     *                            function with the records read.
     * @param containerListHandle the containerListHandle returned by the last read call for this exact list of containers. Will be
     *                            equal to <code>null</code> if this is the first read call for this container list after a connection
     *                            has been established. Driver implementations can optionally use this object to improve the read
     *                            performance.
     * @param samplingGroup       the samplingGroup that was configured for this list of channels that are to be read. Sometimes it may
     *                            be desirable to give the driver a hint on how to group several channels when reading them. This can
     *                            done through the samplingGroup.
     * @return the containerListHandle Object that will passed the next time the same list of channels is to be read.
     * Use this Object as a handle to improve performance or simply return <code>null</code>.
     * @throws UnsupportedOperationException if the method is not implemented by the driver.
     * @throws ConnectionException           if the connection to the device was interrupted.
     */
    public Object read(DeviceConnection connection, List<ChannelRecordContainer> containers,
                       Object containerListHandle, String samplingGroup)
            throws UnsupportedOperationException, ConnectionException;

    /**
     * Starts listening on the given connection for data from the channels that correspond to the given record
     * containers. The list of containers will overwrite the list passed by the previous startListening call. Will
     * notify the given listener of new records that arrive on the data channels.
     *
     * @param connection the device connection that is to be used to listen.
     * @param containers the containers identify the channels to listen on. They will be filled by this function with the
     *                   records received and passed to the listener.
     * @param listener   the listener to inform that new data has arrived.
     * @throws UnsupportedOperationException if the method is not implemented by the driver.
     * @throws ConnectionException           if the connection to the device was interrupted.
     */
    public void startListening(DeviceConnection connection, List<ChannelRecordContainer> containers,
                               RecordsReceivedListener listener)
            throws UnsupportedOperationException, ConnectionException;

    /**
     * Writes the data channels that correspond to the given value containers. The write result is returned by setting
     * the flag in the containers. If the connection to the device is interrupted, then any necessary resources that
     * correspond to this connection should be cleaned up and a <code>ConnectionException</code> shall be thrown.
     *
     * @param connection          the device connection that is to be used to write the channels.
     * @param containers          the containers hold the information of what channels are to be written and the values that are to
     *                            written. They will be filled by this function with a flag stating whether the write process was
     *                            successful or not.
     * @param containerListHandle the containerListHandle returned by the last write call for this exact list of containers. Will be
     *                            equal to <code>null</code> if this is the first read call for this container list after a connection
     *                            has been established. Driver implementations can optionally use this object to improve the write
     *                            performance.
     * @return the containerListHandle Object that will passed the next time the same list of channels is to be written.
     * Use this Object as a handle to improve performance or simply return <code>null</code>.
     * @throws UnsupportedOperationException if the method is not implemented by the driver.
     * @throws ConnectionException           if the connection to the device was interrupted.
     */
    public Object write(DeviceConnection connection,
                        List<ChannelValueContainer> containers,
                        Object containerListHandle)
            throws UnsupportedOperationException, ConnectionException;

}
