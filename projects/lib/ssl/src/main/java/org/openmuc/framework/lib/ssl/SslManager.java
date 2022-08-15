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

import java.io.FileInputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.openmuc.framework.lib.osgi.config.DictionaryPreprocessor;
import org.openmuc.framework.lib.osgi.config.PropertyHandler;
import org.openmuc.framework.lib.osgi.config.ServicePropertyException;
import org.openmuc.framework.security.SslConfigChangeListener;
import org.openmuc.framework.security.SslManagerInterface;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SslManager implements ManagedService, SslManagerInterface {
    private static final Logger logger = LoggerFactory.getLogger(SslManager.class);
    private final List<SslConfigChangeListener> listeners = new ArrayList<>();
    private final PropertyHandler propertyHandler;
    private KeyManagerFactory keyManagerFactory;
    private TrustManagerFactory trustManagerFactory;
    private SSLContext sslContext;
    private boolean loaded = false;

    SslManager() {
        try {
            keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
            trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
            sslContext = SSLContext.getInstance("TLSv1.2");
            sslContext.init(null, null, null);
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            logger.error("Factory could not be loaded: {}", e.getMessage());
        }
        propertyHandler = new PropertyHandler(new Settings(), SslManager.class.getName());
    }

    @Override
    public void listenForConfigChange(SslConfigChangeListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    private void load() {
        loaded = true;
        char[] keyStorePassword = propertyHandler.getString(Settings.KEYSTORE_PASSWORD).toCharArray();
        char[] trustStorePassword = propertyHandler.getString(Settings.TRUSTSTORE_PASSWORD).toCharArray();

        try {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(new FileInputStream(propertyHandler.getString(Settings.KEYSTORE)), keyStorePassword);
            KeyStore trustStore = KeyStore.getInstance("PKCS12");
            trustStore.load(new FileInputStream(propertyHandler.getString(Settings.TRUSTSTORE)), trustStorePassword);

            // get factories
            keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
            keyManagerFactory.init(keyStore, keyStorePassword);
            trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
            trustManagerFactory.init(trustStore);

            sslContext = SSLContext.getInstance("TLSv1.2");
            sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);
            logger.info("Successfully loaded");
        } catch (Exception e) {
            logger.error("Could not load key/trust store: {}", e.getMessage());
        }
    }

    private void notifyListeners() {
        synchronized (listeners) {
            for (SslConfigChangeListener listener : listeners) {
                listener.configChanged();
            }
        }
    }

    @Override
    public KeyManagerFactory getKeyManagerFactory() {
        return keyManagerFactory;
    }

    @Override
    public TrustManagerFactory getTrustManagerFactory() {
        return trustManagerFactory;
    }

    @Override
    public SSLContext getSslContext() {
        return sslContext;
    }

    @Override
    public boolean isLoaded() {
        return loaded;
    }

    void tryProcessConfig(DictionaryPreprocessor newConfig) {
        try {
            propertyHandler.processConfig(newConfig);
            if (!loaded || !propertyHandler.isDefaultConfig() && propertyHandler.configChanged()) {
                load();
                notifyListeners();
            }
        } catch (ServicePropertyException e) {
            logger.error("update properties failed", e);
        }
    }

    @Override
    public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
        DictionaryPreprocessor dict = new DictionaryPreprocessor(properties);
        if (!dict.wasIntermediateOsgiInitCall()) {
            tryProcessConfig(dict);
        }
    }
}
