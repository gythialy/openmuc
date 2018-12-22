/*
 * Copyright 2011-18 Fraunhofer ISE
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
package org.openmuc.framework.server.restws;

import org.openmuc.framework.authentication.AuthenticationService;
import org.openmuc.framework.config.ConfigService;
import org.openmuc.framework.dataaccess.DataAccessService;
import org.openmuc.framework.lib.json.Const;
import org.openmuc.framework.server.restws.servlets.ChannelResourceServlet;
import org.openmuc.framework.server.restws.servlets.ConnectServlet;
import org.openmuc.framework.server.restws.servlets.DeviceResourceServlet;
import org.openmuc.framework.server.restws.servlets.DriverResourceServlet;
import org.openmuc.framework.server.restws.servlets.UserServlet;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.HttpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public final class RestServer {

    private static final Logger logger = LoggerFactory.getLogger(RestServer.class);

    private static DataAccessService dataAccessService;
    private static AuthenticationService authenticationService;
    private static ConfigService configService;
    private static HttpService httpService;

    private final ChannelResourceServlet chRServlet = new ChannelResourceServlet();
    private final DeviceResourceServlet devRServlet = new DeviceResourceServlet();
    private final DriverResourceServlet drvRServlet = new DriverResourceServlet();
    private final ConnectServlet connectServlet = new ConnectServlet();
    private final UserServlet userServlet = new UserServlet();
    // private final ControlsServlet controlsServlet = new ControlsServlet();

    @Activate
    protected void activate(ComponentContext context) throws Exception {
        logger.info("Activating REST Server");

        SecurityHandler securityHandler = new SecurityHandler(context.getBundleContext().getBundle(),
                authenticationService);

        httpService.registerServlet(Const.ALIAS_CHANNELS, chRServlet, null, securityHandler);
        httpService.registerServlet(Const.ALIAS_DEVICES, devRServlet, null, securityHandler);
        httpService.registerServlet(Const.ALIAS_DRIVERS, drvRServlet, null, securityHandler);
        httpService.registerServlet(Const.ALIAS_USERS, userServlet, null, securityHandler);
        httpService.registerServlet(Const.ALIAS_CONNECT, connectServlet, null, securityHandler);
        // httpService.registerServlet(Const.ALIAS_CONTROLS, controlsServlet, null, securityHandler);
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        logger.info("Deactivating REST Server");

        httpService.unregister(Const.ALIAS_CHANNELS);
        httpService.unregister(Const.ALIAS_DEVICES);
        httpService.unregister(Const.ALIAS_DRIVERS);
        httpService.unregister(Const.ALIAS_USERS);
        httpService.unregister(Const.ALIAS_CONNECT);
        // httpService.unregister(Const.ALIAS_CONTROLS);
    }

    @Reference
    protected void setConfigService(ConfigService configService) {
        RestServer.configService = configService;
    }

    protected void unsetConfigService(ConfigService configService) {
        RestServer.configService = null;
    }

    @Reference
    protected void setAuthenticationService(AuthenticationService authenticationService) {
        RestServer.authenticationService = authenticationService;
    }

    protected void unsetAuthenticationService(AuthenticationService authenticationService) {
        RestServer.authenticationService = null;
    }

    @Reference
    protected void setHttpService(HttpService httpService) {
        RestServer.httpService = httpService;
    }

    protected void unsetHttpService(HttpService httpService) {
        RestServer.httpService = null;
    }

    @Reference
    protected void setDataAccessService(DataAccessService dataAccessService) {
        RestServer.dataAccessService = dataAccessService;
    }

    protected void unsetDataAccessService(DataAccessService dataAccessService) {
        RestServer.dataAccessService = null;
    }

    public static DataAccessService getDataAccessService() {
        return RestServer.dataAccessService;
    }

    public static ConfigService getConfigService() {
        return RestServer.configService;
    }

    public static AuthenticationService getAuthenticationService() {
        return RestServer.authenticationService;
    }

}
