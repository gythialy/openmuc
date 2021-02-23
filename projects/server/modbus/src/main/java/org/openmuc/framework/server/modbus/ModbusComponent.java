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

package org.openmuc.framework.server.modbus;

import java.io.IOException;

import org.openmuc.framework.lib.osgi.deployment.RegistrationHandler;
import org.openmuc.framework.server.spi.ServerService;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class ModbusComponent {

    private static Logger logger = LoggerFactory.getLogger(ModbusComponent.class);
    private ModbusServer modbusServer;
    private RegistrationHandler registrationHandler;

    @Activate
    protected void activate(BundleContext context) throws IOException {
        logger.info("Activating Modbus Server");
        modbusServer = new ModbusServer();

        registrationHandler = new RegistrationHandler(context);
        String pid = ModbusServer.class.getName();
        registrationHandler.provideInFramework(ServerService.class.getName(), modbusServer, pid);

    }

    @Deactivate
    protected void deactivate(BundleContext context) {
        logger.info("Deactivating Modbus Server");
        modbusServer.shutdown();
        registrationHandler.removeAllProvidedServices();
    }

}
