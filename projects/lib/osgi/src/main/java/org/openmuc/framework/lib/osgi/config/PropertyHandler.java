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

package org.openmuc.framework.lib.osgi.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Manages properties, performs consistency checks and provides methods to access properties as int, string or boolean
 */
public class PropertyHandler {

    private static final Logger logger = LoggerFactory.getLogger(PropertyHandler.class);
    private static final String DEFAULT_FOLDER = "load/";
    private static final String DEFAULT_PROPERTY_KEY = "felix.fileinstall.dir";
    private final Map<String, ServiceProperty> currentProperties;
    private final String pid;
    private boolean configChanged;
    private boolean defaultConfig;

    /**
     * Constructor
     *
     * @param settings settings
     * @param pid      Name of class implementing ManagedService e.g. String pid = MyClass.class.getName()
     */
    public PropertyHandler(GenericSettings settings, String pid) {
        this.currentProperties = settings.getProperties();
        this.pid = pid;
        configChanged = false;
        defaultConfig = true;
        initializePropertyFile();
    }

    private void initializePropertyFile() {
        // FIXME check ConfigurationFileValidator and PropertyHandler for redundant checks. Maybe they could
        // use same methods?
        checkDirectory();
        // NOTE: Purpose of ConfigurationFileValidator is to initially compare file with settings class.
        // So ConfigurationFileValidator is only called ONCE at the beginning
        PropertyFileValidator serviceConfigurator = new PropertyFileValidator();
        serviceConfigurator.initServiceProperties(currentProperties, pid);
    }

    private void checkDirectory() {
        String propertyDir = System.getProperty(DEFAULT_PROPERTY_KEY);
        if (propertyDir == null) {
            propertyDir = DEFAULT_FOLDER;
            System.setProperty(DEFAULT_PROPERTY_KEY, DEFAULT_FOLDER);
            logger.warn("Missing systems.property for felix file install. Using default: \"{}={}\"",
                    DEFAULT_PROPERTY_KEY, DEFAULT_FOLDER);
        }
        Path path = Paths.get(propertyDir);
        if (!Files.exists(path)) {
            new File(propertyDir).mkdir();
        }
    }

    public void processConfig(DictionaryPreprocessor newConfig) throws ServicePropertyException {
        Dictionary<String, String> newDictionary = newConfig.getCleanedUpDeepCopyOfDictionary();
        validateKeys(newDictionary);

        HashMap<String, String> oldProperties = getCopyOfProperties();

        Enumeration<String> keys = newDictionary.keys();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            ServiceProperty property = currentProperties.get(key);
            String dictValue = newDictionary.get(key);
            applyNewValue(dictValue, key, property);
        }

        configChanged = hasConfigChanged(oldProperties);
        if (defaultConfig && configChanged) {
            defaultConfig = false;
        }
    }

    private void applyNewValue(String newDictValue, String key, ServiceProperty property)
            throws ServicePropertyException {

        if (property.isMandatory()) {
            processMandatoryProperty(newDictValue, key, property);
        } else {
            property.update(newDictValue);
        }
    }

    private void processMandatoryProperty(String newDictValue, String key, ServiceProperty property)
            throws ServicePropertyException {

        if (newDictValue == null) {
            throw new ServicePropertyException("mandatory property '" + key + "' value is null");
        }

        if (newDictValue.equals("")) {
            throw new ServicePropertyException("mandatory property '" + key + "' is empty");
        } else {
            property.update(newDictValue);
        }
    }

    private void validateKeys(Dictionary<String, ?> newDictionary) throws ServicePropertyException {
        checkForUnknownKeys(newDictionary);
        checkForMissingKeys(newDictionary);
    }

    /**
     * validate new keys (given by updated(dictionary...) against original keys (given by settings class)
     */
    private void checkForMissingKeys(Dictionary<String, ?> newDictionary) throws ServicePropertyException {
        for (String originalKey : currentProperties.keySet()) {
            if (Collections.list(newDictionary.keys()).stream().noneMatch(t -> t.equals(originalKey))) {
                throw new ServicePropertyException(
                        "Missing Property: updated property dictionary doesn't contain key " + originalKey);
            }
        }
    }

    /**
     * validate original keys (given by settings class) against new keys (given by updated(dictionary...)
     */
    private void checkForUnknownKeys(Dictionary<String, ?> newDictionary) throws ServicePropertyException {
        Enumeration<String> newKeys = newDictionary.keys();
        while (newKeys.hasMoreElements()) {
            String newKey = newKeys.nextElement();
            if (!currentProperties.containsKey(newKey)) {
                throw new ServicePropertyException("Unknown Property: '" + newKey
                        + "' New property key has been introduced, which is not part of settings class for " + pid);
            }
        }
    }

    private boolean hasConfigChanged(HashMap<String, String> oldProperties) {
        for (Map.Entry<String, String> oldSet : oldProperties.entrySet()) {
            String oldKey = oldSet.getKey();
            String oldValue = oldSet.getValue();
            ServiceProperty property = currentProperties.get(oldKey);
            String newValue = property.getValue();
            if (!oldValue.equals(newValue)) {
                return true;
            }
        }
        return false;
    }

    public int getInt(String key) {
        ServiceProperty prop = currentProperties.get(key);
        return Integer.valueOf(prop.getValue());
    }

    public double getDouble(String key) {
        ServiceProperty prop = currentProperties.get(key);
        return Double.valueOf(prop.getValue());
    }

    public String getString(String key) {
        return currentProperties.get(key).getValue();
    }

    public boolean getBoolean(String key) {
        ServiceProperty prop = currentProperties.get(key);
        return Boolean.valueOf(prop.getValue());
    }

    /**
     * @return a HashMap with value from type String not ServiceProperty! Full ServiceProperty object not necessary
     * here.
     */
    private HashMap<String, String> getCopyOfProperties() {
        HashMap<String, String> oldProperties = new HashMap<>();
        for (Map.Entry<String, ServiceProperty> oldSet : currentProperties.entrySet()) {
            String oldKey = oldSet.getKey();
            String oldValue = oldSet.getValue().getValue();
            oldProperties.put(oldKey, oldValue);
        }
        return oldProperties;
    }

    public Map<String, ServiceProperty> getCurrentProperties() {
        return currentProperties;
    }

    /**
     * @return <b>true</b> as long as the properties are identical to the one that were given to the constructor,
     * otherwise <b>false</b>
     */
    public boolean isDefaultConfig() {
        return defaultConfig;
    }

    /**
     * @return <b>true</b> if config has changed after last {@link #processConfig(DictionaryPreprocessor)} call,
     * otherwise <b>false</b>
     */
    public boolean configChanged() {
        return configChanged;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, ServiceProperty> entry : currentProperties.entrySet()) {
            String key = entry.getKey();
            ServiceProperty propValue = entry.getValue();
            sb.append("\n" + key + "=" + propValue.getValue());
        }
        return sb.toString();
    }

}
