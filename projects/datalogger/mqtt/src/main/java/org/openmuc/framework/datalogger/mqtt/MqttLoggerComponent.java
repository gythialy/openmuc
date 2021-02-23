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

package org.openmuc.framework.datalogger.mqtt;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.openmuc.framework.datalogger.spi.DataLoggerService;
import org.openmuc.framework.lib.osgi.deployment.RegistrationHandler;
import org.openmuc.framework.parser.spi.ParserService;
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
public class MqttLoggerComponent {

    private static final Logger logger = LoggerFactory.getLogger(MqttLoggerComponent.class);
    private RegistrationHandler registrationHandler;
    private MqttLogger mqttLogger;

    @Activate
    protected void activate(BundleContext context) {
        logger.info("Activating MQTT Logger");
        mqttLogger = new MqttLogger();
        registrationHandler = new RegistrationHandler(context);

        // subscribe for ParserService
        String serviceName = ParserService.class.getName();
        registrationHandler.subscribeForServiceServiceEvent(serviceName, (event) -> {
            handleServiceRegistrationEvent(event, context);
        });

        // provide DataLoggerService and MangedService
        String pid = MqttLogger.class.getName();
        registrationHandler.provideInFramework(DataLoggerService.class.getName(), mqttLogger, pid);
    }

    private void handleServiceRegistrationEvent(Object event, BundleContext context) {
        ServiceReference<?> serviceReference = ((ServiceEvent) event).getServiceReference();

        String parserId = (String) serviceReference.getProperty("parserID");
        ParserService parserService = (ParserService) context.getService(serviceReference);
        String parserServiceName = parserService.getClass().getName();

        if (((ServiceEvent) event).getType() == ServiceEvent.UNREGISTERING) {
            logger.info("{} unregistering, removing Parser", parserServiceName);
            mqttLogger.removeParser(parserId);
        }
        else {
            logger.info("{} changed, updating Parser", parserServiceName);
            mqttLogger.addParser(parserId, parserService);
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext context) throws IOException, TimeoutException {
        logger.info("Deactivating MQTT logger");
        mqttLogger.shutdown();
    }

}
