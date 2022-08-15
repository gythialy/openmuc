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

package org.openmuc.framework.lib.osgi.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.Dictionary;
import java.util.Hashtable;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PropertyHandlerTest {

    private final String URL = "url";
    private final String USER = "user";
    private final String PASSWORD = "password";
    private final String pid = "org.openmuc.framework.MyService";
    private final String propertyDir = "properties/";
    private File loadDir;
    private Dictionary<String, String> changedDic;
    private Dictionary<String, String> defaultDic;
    private Settings settings;

    @BeforeEach
    void initProperties() {
        System.setProperty("felix.fileinstall.dir", propertyDir);
        loadDir = new File(propertyDir);
        loadDir.mkdir();
        settings = new Settings();
    }

    @BeforeEach
    void initChangedDic() {
        changedDic = new Hashtable<>();
        changedDic.put(URL, "postgres");
        changedDic.put(USER, "openbug");
        changedDic.put(PASSWORD, "openmuc");
    }

    @BeforeEach
    void initDefaultDic() {
        defaultDic = new Hashtable<>();
        defaultDic.put(URL, "jdbc:h2");
        defaultDic.put(USER, "openmuc");
        defaultDic.put(PASSWORD, "openmuc");
    }

    @AfterEach
    void cleanUp() {
        loadDir.delete();
    }

    @Test
    void throwExceptionIfPropertyInDicIsMissing() {
        changedDic = new Hashtable<>();
        PropertyHandler propertyHandler = new PropertyHandler(settings, pid);
        changedDic.put(URL, "postgres");
        changedDic.put(USER, "openbug");
        DictionaryPreprocessor dict = new DictionaryPreprocessor(changedDic);
        assertThrows(ServicePropertyException.class, () -> propertyHandler.processConfig(dict));
    }

    @Test
    void isDefaultAfterStartWithChangedConfig_false() throws ServicePropertyException {
        PropertyHandler propertyHandler = new PropertyHandler(settings, pid);
        DictionaryPreprocessor config = new DictionaryPreprocessor(changedDic);
        propertyHandler.processConfig(config);
        assertFalse(propertyHandler.isDefaultConfig());
    }

    @Test
    void configChangedAfterStartWithChangedConfig_true() throws ServicePropertyException {
        PropertyHandler propertyHandler = new PropertyHandler(settings, pid);
        DictionaryPreprocessor config = new DictionaryPreprocessor(changedDic);
        propertyHandler.processConfig(config);
        assertTrue(propertyHandler.configChanged());
    }

    @Test
    void isDefaultAfterStartWithDefaultConfig_true() throws ServicePropertyException {
        PropertyHandler propertyHandler = new PropertyHandler(settings, pid);
        DictionaryPreprocessor config = new DictionaryPreprocessor(defaultDic);
        propertyHandler.processConfig(config);
        assertTrue(propertyHandler.isDefaultConfig());
    }

    @Test
    void configChangedAfterStartWithDefaultConfig_false() throws ServicePropertyException {
        PropertyHandler propertyHandler = new PropertyHandler(settings, pid);
        DictionaryPreprocessor config = new DictionaryPreprocessor(defaultDic);
        propertyHandler.processConfig(config);
        assertFalse(propertyHandler.configChanged());
    }

    @Test
    void toStringDoesNotShowPassword() {
        PropertyHandler propertyHandler = new PropertyHandler(settings, pid);
        DictionaryPreprocessor config = new DictionaryPreprocessor(defaultDic);

        assertTrue(!propertyHandler.toString().contains("password=openmuc"));
        assertTrue(propertyHandler.toString().contains("password=*****"));
    }

    @Test
    void noMoreNullPointerExceptions() {
        PropertyHandler propertyHandler = new PropertyHandler(settings, pid);
        DictionaryPreprocessor config = new DictionaryPreprocessor(defaultDic);
        assertFalse(propertyHandler.hasValueForKey("thisDoesNotExist"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> propertyHandler.getBoolean("thisDoesNotExist"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> propertyHandler.getDouble("thisDoesNotExist"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> propertyHandler.getInt("thisDoesNotExist"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> propertyHandler.getString("thisDoesNotExist"));
    }

    class Settings extends GenericSettings {
        public Settings() {
            super();
            properties.put(URL, new ServiceProperty(URL, "URL of the used database", "jdbc:h2", true));
            properties.put(USER, new ServiceProperty(USER, "User of the used database", "openmuc", true));
            properties.put(PASSWORD, new ServiceProperty(PASSWORD, "Password for the database user", "openmuc", true));
        }
    }

}
