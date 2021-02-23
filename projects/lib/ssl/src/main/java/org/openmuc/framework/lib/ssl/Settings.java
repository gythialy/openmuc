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
import org.openmuc.framework.lib.osgi.config.GenericSettings;
import org.openmuc.framework.lib.osgi.config.ServiceProperty;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;

import java.util.Dictionary;

public class Settings extends GenericSettings implements ManagedService {
    final static String KEYSTORE = "keystore";
    final static String KEYSTORE_PASSWORD = "keystorepassword";
    final static String TRUSTSTORE = "truststore";
    final static String TRUSTSTORE_PASSWORD = "truststorepassword";

    Settings() {
        super();
        properties.put(KEYSTORE, new ServiceProperty(KEYSTORE, "path to the keystore", "conf/keystore.jks", true));
        properties.put(KEYSTORE_PASSWORD,
                new ServiceProperty(KEYSTORE_PASSWORD, "keystore password", "changeme", true));
        properties.put(TRUSTSTORE,
                new ServiceProperty(TRUSTSTORE, "path to the truststore", "conf/truststore.jks", true));
        properties.put(TRUSTSTORE_PASSWORD,
                new ServiceProperty(TRUSTSTORE_PASSWORD, "truststore password", "changeme", true));
    }

    @Override
    public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
        DictionaryPreprocessor dict = new DictionaryPreprocessor(properties);
        if (!dict.wasIntermediateOsgiInitCall()) {
            SslManager.getInstance().tryProcessConfig(dict);
        }
    }
}
