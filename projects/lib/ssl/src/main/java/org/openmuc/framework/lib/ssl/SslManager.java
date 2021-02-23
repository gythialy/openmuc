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

package org.openmuc.framework.lib.ssl;

import org.openmuc.framework.lib.osgi.config.DictionaryPreprocessor;
import org.openmuc.framework.lib.osgi.config.PropertyHandler;
import org.openmuc.framework.lib.osgi.config.ServicePropertyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class SslManager {
    private static final Logger logger = LoggerFactory.getLogger(SslManager.class);
    private final List<SslConfigChangeListener> listeners = new ArrayList<>();
    private KeyManagerFactory keyManagerFactory;
    private TrustManagerFactory trustManagerFactory;
    private SSLContext sslContext;
    private PropertyHandler propertyHandler;

    private SslManager() {
        try {
            keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
            trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
            sslContext = SSLContext.getInstance("TLSv1.2");
            sslContext.init(null, null, null);
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            logger.error("Factory could not be loaded: {}", e.getMessage());
        }
    }

    public static SslManager getInstance() {
        return LazySslManager.INSTANCE;
    }

    public void listenForConfigChange(SslConfigChangeListener listener) {
        listeners.add(listener);
    }

    private void load() {
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
        for (SslConfigChangeListener listener : listeners) {
            listener.configChanged();
        }
    }

    public KeyManagerFactory getKeyManagerFactory() {
        return keyManagerFactory;
    }

    public TrustManagerFactory getTrustManagerFactory() {
        return trustManagerFactory;
    }

    public SSLContext getSslContext() {
        return sslContext;
    }

    void tryProcessConfig(DictionaryPreprocessor newConfig) {
        try {
            propertyHandler.processConfig(newConfig);
            if (!propertyHandler.isDefaultConfig() && propertyHandler.configChanged()) {
                load();
                notifyListeners();
            }
        } catch (ServicePropertyException e) {
            logger.error("update properties failed", e);
        }
    }

    void newSettings(Settings settings) {
        propertyHandler = new PropertyHandler(settings, SslManager.class.getName());
        load();
        notifyListeners();
    }

    private static class LazySslManager {
        static final SslManager INSTANCE = new SslManager();
    }
}
