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

package org.openmuc.framework.lib.osgi.deployment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ManagedService;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;

/**
 * This class provides some methods to ease the dynamic handling of OSGi services. It capsules routines of the OSGi
 * service management to make the handling more convenient for the developer. This class provides methods to register
 * own services to the OSGi environment and methods to subscribe to existing services.
 */
public class RegistrationHandler implements ServiceListener {

    private final BundleContext context;
    private final List<ServiceRegistration<?>> registrations;
    private final Logger logger = (Logger) LoggerFactory.getLogger(this.getClass());
    private final Map<String, ServiceAccess> subscribedServices;
    private final Map<String, ServiceAccess> subscribedServiceEvents;
    private final List<String> filterEntries;
    private final String FELIX_FILE_INSTALL_KEY = "felix.fileinstall.filename";
    private ConfigurationAdmin configurationAdmin;

    /**
     * Constructor
     *
     * @param context
     *            BundleContext of your OSGi component which is typically provided in the activate method of your
     *            component.
     */
    public RegistrationHandler(BundleContext context) {
        this.context = context;
        registrations = new ArrayList<>();
        subscribedServices = new HashMap<>();
        subscribedServiceEvents = new HashMap<>();
        filterEntries = new ArrayList<>();
        subscribeForService(ConfigurationAdmin.class.getName(),
                instance -> configurationAdmin = (ConfigurationAdmin) instance);

    }

    /**
     * Provides the given service within the OSGi environment.
     *
     * @param serviceName
     *            Name of the service to provide. Typically "MyService.class.getName()" This class must implement the
     *            interface org.osgi.service.cm.ManagedService.
     * @param serviceInstance
     *            Instance of the service
     * @param pid
     *            Persistence Id. Typically package path + class name e.g. "my.package.path.myClass"
     */
    public void provideInFramework(String serviceName, Object serviceInstance, String pid) {
        Dictionary<String, Object> properties = buildProperties(pid);

        ServiceRegistration<?> newRegistration = context.registerService(serviceName, serviceInstance, properties);
        ServiceRegistration<?> newManagedService = context.registerService(ManagedService.class.getName(),
                serviceInstance, properties);
        updateConfigDatabaseWithGivenDictionary(properties);

        registrations.add(newRegistration);
        registrations.add(newManagedService);
    }

    public void provideInFrameworkWithoutManagedService(String serviceName, Object serviceInstance, String pid) {
        Dictionary<String, Object> properties = buildProperties(pid);
        ServiceRegistration<?> newRegistration = context.registerService(serviceName, serviceInstance, properties);
        updateConfigDatabaseWithGivenDictionary(properties);
        registrations.add(newRegistration);
    }

    public void provideInFrameworkAsManagedService(Object serviceInstance, String pid) {
        Dictionary<String, Object> properties = buildProperties(pid);

        ServiceRegistration<?> newManagedService = context.registerService(ManagedService.class.getName(),
                serviceInstance, properties);
        updateConfigDatabaseWithGivenDictionary(properties);

        registrations.add(newManagedService);
    }

    public void provideInFrameworkWithoutConfiguration(String serviceName, Object serviceInstance) {
        ServiceRegistration<?> newRegistration = context.registerService(serviceName, serviceInstance, null);

        registrations.add(newRegistration);
    }

    /**
     * Provides the given service within the OSGi environment with initial properties. <br>
     * <b>NOTE:</b> This method can be used at early development stage when no deployment package exists. Later on the
     * service would get the properties via the ManagedService interface.
     *
     * @param serviceName
     *            Name of the service to provide. Typically "MyService.class.getName()"
     * @param serviceInstance
     *            Instance of the service
     * @param properties
     *            The properties for this service.
     */
    public void provideWithInitProperties(String serviceName, Object serviceInstance,
            Dictionary<String, Object> properties) {

        ServiceRegistration<?> newRegistration = context.registerService(serviceName, serviceInstance, properties);
        updateConfigDatabaseWithGivenDictionary(properties);
        registrations.add(newRegistration);
    }

    /**
     * Updates configuration entry in framework database for given dictionary. Dictionary must contain a property with
     * "Constants.SERVICE_PID" as key and service pid as value.
     *
     * @param properties
     *            dictionary with updated properties and service pid
     */
    public void updateConfigDatabaseWithGivenDictionary(Dictionary<String, Object> properties) {
        String pid = null;
        try {
            pid = (String) properties.get(Constants.SERVICE_PID);
            Configuration newConfig = configurationAdmin.getConfiguration(pid);

            Dictionary<String, ?> existingProperties = newConfig.getProperties();
            if (existingProperties != null) {
                String fileName = (String) existingProperties.get(FELIX_FILE_INSTALL_KEY);
                if (fileName != null) {
                    properties.put(FELIX_FILE_INSTALL_KEY, fileName);
                }
            }

            newConfig.update(properties);
        } catch (IOException e) {
            logger.error("Config for {} can not been built\n{}", pid, e.getMessage());
        }
    }

    /**
     * Unregisters all provided services
     */
    public void removeAllProvidedServices() {
        for (ServiceRegistration<?> registration : registrations) {
            registration.unregister();
        }
        context.removeServiceListener(this);
    }

    /**
     * Subscribe for a service.
     *
     * @param serviceName
     *            Name of the service. Typically "MyService.class.getName(). This class must implement the interface
     *            org.osgi.service.cm.ManagedService.
     * @param access
     *            ServicAccess instance
     */
    public void subscribeForService(String serviceName, ServiceAccess access) {
        subscribedServices.put(serviceName, access);
        filterEntries.add(serviceName);
        updateServiceListener();
        updateNow();
    }

    public void subscribeForServiceServiceEvent(String serviceName, ServiceAccess access) {
        subscribedServiceEvents.put(serviceName, access);
        filterEntries.add(serviceName);
        updateServiceListener();
        updateNow();
    }

    private Dictionary<String, Object> buildProperties(String pid) {
        Dictionary<String, Object> properties = new Hashtable<>();
        properties.put(Constants.SERVICE_PID, pid);
        String felixFileDir = System.getProperty("felix.fileinstall.dir");
        properties.put(FELIX_FILE_INSTALL_KEY, felixFileDir);

        return properties;
    }

    private void updateServiceListener() {
        context.removeServiceListener(this);
        String serviceFilter = createFilter();
        try {
            context.addServiceListener(this, serviceFilter);
        } catch (InvalidSyntaxException e) {
            logger.error("Service listener can't be added to framework", e);
        }
    }

    private String createFilter() {
        StringBuilder builder = new StringBuilder();

        builder.append("( |");
        for (String serviceName : filterEntries) {
            builder.append("(" + Constants.OBJECTCLASS + "=" + serviceName + ") ");
        }

        builder.append(" ) ");
        return builder.toString();
    }

    private void updateNow() {
        for (String serviceName : subscribedServices.keySet()) {
            ServiceReference<?> serviceRef = context.getServiceReference(serviceName);
            ServiceAccess access = subscribedServices.get(serviceName);

            if (serviceRef == null) {
                access.setService(null);
            }
            else {
                access.setService(context.getService(serviceRef));
            }
        }
    }

    /**
     * Internal method! Must not be used by the user of RegistrationHandler class. (Note: Due to the implementation of
     * ServiceListener this method must be public)
     */
    @Override
    public void serviceChanged(ServiceEvent event) {
        serviceServiceEventSubscribers(event);
        serveServiceSubscribers(event);
    }

    private void serviceServiceEventSubscribers(ServiceEvent event) {
        String changedServiceClass = ((String[]) event.getServiceReference().getProperty("objectClass"))[0];
        ServiceAccess access = subscribedServiceEvents.get(changedServiceClass);

        if (access != null) {
            access.setService(event);
        }
    }

    private void serveServiceSubscribers(ServiceEvent event) {
        String changedServiceClass = ((String[]) event.getServiceReference().getProperty("objectClass"))[0];
        logger.debug("service changed for class " + changedServiceClass);
        ServiceAccess access = subscribedServices.get(changedServiceClass);

        if (access == null) {
            return;
        }

        if (event.getType() == ServiceEvent.UNREGISTERING) {
            access.setService(null);
            return;
        }

        ServiceReference<?> serviceRef = context.getServiceReference(changedServiceClass);
        access.setService(context.getService(serviceRef));
    }
}
