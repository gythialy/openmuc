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
package org.openmuc.framework.server.restws;

import org.openmuc.framework.authentication.AuthenticationService;
import org.openmuc.framework.config.ConfigService;
import org.openmuc.framework.dataaccess.DataAccessService;
import org.openmuc.framework.server.restws.servlets.AddDriverServlet;
import org.openmuc.framework.server.restws.servlets.ChannelResourceServlet;
import org.openmuc.framework.server.restws.servlets.DeviceResourceServlet;
import org.openmuc.framework.server.restws.servlets.DriverResourceServlet;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.HttpService;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Activator {

    private final static Logger logger = LoggerFactory.getLogger(Activator.class);

    private static DataAccessService dataAccess = null;
    private static ConfigService configService;

    private volatile HttpService httpService;

    private ServiceTracker<HttpService, HttpService> httpTracker;

    private volatile ChannelResourceServlet chRServlet;
    private volatile DeviceResourceServlet devRServlet;
    private volatile DriverResourceServlet drvRServlet;
    private volatile AddDriverServlet addDrvServlet;

    protected void activate(ComponentContext context) throws Exception {

        logger.info("Activating REST SERVER");

        chRServlet = new ChannelResourceServlet();
        devRServlet = new DeviceResourceServlet();
        drvRServlet = new DriverResourceServlet();
        addDrvServlet = new AddDriverServlet();

        registerServlets();

        httpTracker = new ServiceTracker<HttpService, HttpService>(context.getBundleContext(),
                                                                   HttpService.class.getName(),
                                                                   null);
        httpTracker.open();
        HttpService httpServiceAct = httpTracker.getService();

        if (httpServiceAct != null && this.httpService == null) {
            setHttpService(httpServiceAct);
        }
    }

    private synchronized void registerServlets() {
        if (httpService != null && chRServlet != null && devRServlet != null && drvRServlet != null
            && addDrvServlet != null) {
            try {
                httpService.registerServlet("/rest/channel", chRServlet, null, null);
                httpService.registerServlet("/rest/device", devRServlet, null, null);
                httpService.registerServlet("/rest/driver", drvRServlet, null, null);
                httpService.registerServlet("/rest/addDriver", addDrvServlet, null, null);

            }
            catch (Exception e) {
            }
        }
    }

    protected void setConfigService(ConfigService configService) {
        Activator.configService = configService;
    }

    protected void unsetConfigService(ConfigService configService) {
        Activator.configService = null;
    }

    protected void setHttpService(HttpService httpService) {
        this.httpService = httpService;
        registerServlets();
    }

    protected void unsetHttpService(HttpService httpService) {
        httpService.unregister("/rest/channel");
        httpService.unregister("/rest/device");
        httpService.unregister("/rest/driver");
        httpService.unregister("/rest/addDriver");
        this.httpService = null;
    }

    protected void setDataAccessService(DataAccessService dataAccessService) {
        dataAccess = dataAccessService;
    }

    protected void unsetDataAccessService(DataAccessService dataAccessService) {
        dataAccess = null;
    }

    protected void setAuthenticationService(AuthenticationService authenticationService) {
    }

    protected void unsetAuthenticationService(AuthenticationService authenticationService) {
    }

    public static DataAccessService getDataAccess() {
        if (dataAccess == null) {
            throw new NullPointerException();
        }

        return dataAccess;
    }

    public static ConfigService getConfigService() {
        if (configService == null) {
            throw new NullPointerException();
        }

        return configService;
    }

}
