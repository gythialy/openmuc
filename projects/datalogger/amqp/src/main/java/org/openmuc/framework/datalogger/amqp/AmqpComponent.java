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

package org.openmuc.framework.datalogger.amqp;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.openmuc.framework.datalogger.spi.DataLoggerService;
import org.openmuc.framework.lib.osgi.deployment.RegistrationHandler;
import org.openmuc.framework.parser.spi.ParserService;
import org.openmuc.framework.security.SslManagerInterface;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class AmqpComponent {

    private static final Logger logger = LoggerFactory.getLogger(AmqpComponent.class);
    private RegistrationHandler registrationHandler;
    private AmqpLogger amqpLogger;

    @Activate
    protected void activate(BundleContext context) {
        logger.info("Activating Amqp logger");
        amqpLogger = new AmqpLogger();

        registrationHandler = new RegistrationHandler(context);
        String serviceName = ParserService.class.getName();
        registrationHandler.subscribeForServiceServiceEvent(serviceName, (event) -> {
            handleServiceRegistrationEvent(event, context);
        });

        // subscribe for SSLManager
        serviceName = SslManagerInterface.class.getName();
        registrationHandler.subscribeForService(serviceName, instance -> {
            if (instance != null) {
                amqpLogger.setSslManager((SslManagerInterface) instance);
            }
        });

        String pid = AmqpLogger.class.getName();
        registrationHandler.provideInFramework(DataLoggerService.class.getName(), amqpLogger, pid);
    }

    private void handleServiceRegistrationEvent(Object event, BundleContext context) {
        ServiceReference<?> serviceReference = ((ServiceEvent) event).getServiceReference();

        String parserId = (String) serviceReference.getProperty("parserID");
        ParserService parserService = (ParserService) context.getService(serviceReference);
        String parserServiceName = parserService.getClass().getName();

        if (((ServiceEvent) event).getType() == ServiceEvent.UNREGISTERING) {
            logger.info("{} unregistering, removing Parser", parserServiceName);
            amqpLogger.removeParser(parserId);
        }
        else {
            logger.info("{} changed, updating Parser", parserServiceName);
            amqpLogger.addParser(parserId, parserService);
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext context) throws IOException, TimeoutException {
        logger.info("Deactivating Amqp logger");
        amqpLogger.shutdown();
    }
}
