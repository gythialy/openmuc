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

package org.openmuc.framework.driver.amqp;

import org.openmuc.framework.config.ArgumentSyntaxException;
import org.openmuc.framework.config.DriverInfo;
import org.openmuc.framework.config.ScanException;
import org.openmuc.framework.config.ScanInterruptedException;
import org.openmuc.framework.driver.spi.Connection;
import org.openmuc.framework.driver.spi.ConnectionException;
import org.openmuc.framework.driver.spi.DriverDeviceScanListener;
import org.openmuc.framework.driver.spi.DriverService;
import org.openmuc.framework.parser.spi.ParserService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class AmqpDriver implements DriverService {
    private static final Logger logger = LoggerFactory.getLogger(AmqpDriver.class);
    private BundleContext context;
    private AmqpDriverConnection connection;

    @Activate
    public void activate(BundleContext context) {
        this.context = context;
    }

    @Deactivate
    public void deactivate() {
        if (connection != null) {
            connection.disconnect();
        }
    }

    @Override
    public DriverInfo getInfo() {
        final String ID = "amqp";
        final String DESCRIPTION = "Driver to read out of amqp queue";
        final String DEVICE_ADDRESS = "amqp host e.g. localhost, 192.168.8.4, ...";
        final String DEVICE_SETTINGS = "port=<port>;vhost=<vhost>;user=<user>;password=<pw>;framework=<framework_name>;parser=<parser_name>"
                + "[;buffersize=<buffersize>][;ssl=<true/false>][;separator=<separator_sign>][;exchange=<exchange_name>]";
        final String CHANNEL_ADDRESS = "<channel>";
        final String DEVICE_SCAN_SETTINGS = "device scan not supported";

        return new DriverInfo(ID, DESCRIPTION, DEVICE_ADDRESS, DEVICE_SETTINGS, CHANNEL_ADDRESS, DEVICE_SCAN_SETTINGS);
    }

    @Override
    public void scanForDevices(String settings, DriverDeviceScanListener listener)
            throws UnsupportedOperationException, ArgumentSyntaxException, ScanException, ScanInterruptedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void interruptDeviceScan() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Connection connect(String deviceAddress, String settings)
            throws ArgumentSyntaxException, ConnectionException {

        synchronized (this) {
            connection = new AmqpDriverConnection(deviceAddress, settings);

            checkForExistingParserService();
            addParserServiceListenerToServiceRegistry();

            return connection;
        }
    }

    private void checkForExistingParserService() {
        ServiceReference<?> serviceReferenceInit = context.getServiceReference(ParserService.class.getName());

        if (serviceReferenceInit != null) {
            String parserIdInit = (String) serviceReferenceInit.getProperty("parserID");
            ParserService parserInit = (ParserService) context.getService(serviceReferenceInit);
            if (parserInit != null) {
                logger.info("{} registered, updating Parser in AmqpDriver", parserInit.getClass().getName());
                connection.setParser(parserIdInit, parserInit);
            }
        }
    }

    private void addParserServiceListenerToServiceRegistry() {
        String filter = '(' + Constants.OBJECTCLASS + '=' + ParserService.class.getName() + ')';

        try {
            context.addServiceListener(event -> {
                getNewParserImplementationFromServiceRegistry(event);
            }, filter);
        } catch (InvalidSyntaxException e) {
            logger.error("Service listener can't be added to framework", e);
        }
    }

    private void getNewParserImplementationFromServiceRegistry(ServiceEvent event) {
        ServiceReference<?> serviceReference = event.getServiceReference();
        ParserService parser = (ParserService) context.getService(serviceReference);

        String parserId = (String) serviceReference.getProperty("parserID");

        if (event.getType() == ServiceEvent.UNREGISTERING) {
            logger.info("{} unregistering, removing Parser from AmqpDriver", parser.getClass().getName());
            connection.setParser(parserId, null);
        }
        else {
            logger.info("{} changed, updating Parser in AmqpDriver", parser.getClass().getName());
            connection.setParser(parserId, parser);
        }
    }
}
