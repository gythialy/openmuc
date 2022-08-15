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

package org.openmuc.framework.server.restws.servlets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PropertyReader {

    private static final Logger logger = LoggerFactory.getLogger(PropertyReader.class);
    private static final String SEPERATOR = ";";

    private static PropertyReader instance;
    // Map<ORIGIN, [METHODS, HEADERS]>
    private Map<String, ArrayList<String>> propertyMap;
    private boolean enableCors;

    public static PropertyReader getInstance() {
        if (instance == null) {
            instance = new PropertyReader();
        }
        return instance;
    }

    private PropertyReader() {
        loadAllProperties();
    }

    private void loadAllProperties() {
        propertyMap = new HashMap<>();
        enableCors = Boolean.parseBoolean(getProperty("enable_cors"));

        if (enableCors) {
            String[] urls = getPropertyList("url_cors");
            String[] methods = getPropertyList("methods_cors");
            String[] headers = getPropertyList("headers_cors");
            for (int i = 0; i < urls.length; i++) {
                ArrayList<String> methodHeader = new ArrayList<>();
                methodHeader.add(methods[i]);
                methodHeader.add(headers[i]);
                propertyMap.put(urls[i], methodHeader);
            }
        }
    }

    private String[] getPropertyList(String key) {
        return getProperty(key).split(SEPERATOR);
    }

    private String getProperty(String key) {
        String baseKey = "org.openmuc.framework.server.restws.";
        String property;
        try {
            property = System.getProperty(baseKey + key);
        } catch (Exception e) {
            logger.error("Necessary system properties for CORS handling are missing. {}{}", baseKey, key);
            enableCors = false;
            property = "";
        }
        return property;
    }

    public Map<String, ArrayList<String>> getPropertyMap() {
        return propertyMap;
    }

    public boolean isCorsEnabled() {
        return enableCors;
    }
}
