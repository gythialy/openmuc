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
package org.openmuc.framework.lib.ssl;

import org.openmuc.framework.lib.osgi.deployment.RegistrationHandler;
import org.openmuc.framework.security.SslManagerInterface;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class SslManagerComponent {
    private static final Logger logger = LoggerFactory.getLogger(SslManagerComponent.class);

    @Activate
    protected void activate(BundleContext context) {
        logger.info("SSL Component activated");
        RegistrationHandler registrationHandler = new RegistrationHandler(context);
        registrationHandler.provideInFramework(SslManagerInterface.class.getName(), new SslManager(),
                SslManager.class.getName());
    }
}
