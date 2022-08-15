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

package org.openmuc.framework.datalogger.sql;

import org.openmuc.framework.datalogger.spi.DataLoggerService;
import org.openmuc.framework.lib.osgi.deployment.RegistrationHandler;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

@Component
public class SqlLoggerComponent {

    private RegistrationHandler registrationHandler;
    private SqlLoggerService loggerService;

    @Activate
    public void activate(BundleContext bundleContext) {
        registrationHandler = new RegistrationHandler(bundleContext);
        loggerService = new SqlLoggerService();
        String pid = SqlLoggerService.class.getName();
        registrationHandler.provideInFramework(DataLoggerService.class.getName(), loggerService, pid);
    }

    @Deactivate
    public void deactivate() {
        registrationHandler.removeAllProvidedServices();
        loggerService.shutdown();
    }
}
