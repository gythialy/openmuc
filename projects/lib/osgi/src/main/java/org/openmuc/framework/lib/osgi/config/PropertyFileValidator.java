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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;

/**
 * Validates the config file for the registered pid e.g. load/org.openmuc.framework.myproject.MyClass.cfg
 */
public class PropertyFileValidator {

    private static final String RESOURCE_DIR = System.getProperty("felix.fileinstall.dir") + "/";
    private final Logger logger = (Logger) LoggerFactory.getLogger(this.getClass());
    private Map<String, ServiceProperty> serviceProperties;
    private String pid;
    private List<String> existingProperties;
    private String filename;

    public void initServiceProperties(Map<String, ServiceProperty> serviceProperties, String pid) {

        this.pid = pid;
        this.serviceProperties = serviceProperties;

        new File(RESOURCE_DIR).mkdir();
        filename = RESOURCE_DIR + pid + ".cfg";
        // File f = new File("load/" + pid + ".cfg");
        File f = new File(filename);
        if (!f.exists()) {
            writePropertyFile();
        }
        else {
            readExistingProperties();
            checkForMissingPropertiesInFile();
            checkForUnsetPropertiesInFile();
            checkForDeprecatedProperties();
        }
    }

    private void writePropertyFile() {
        try {
            logger.warn("New empty config file: {}", filename);
            FileWriter myWriter = new FileWriter(filename);

            for (ServiceProperty property : serviceProperties.values()) {
                myWriter.write(property.toString());
            }

            myWriter.close();
        } catch (IOException e) {
            logger.error("Failed to write property file", e);
        }
    }

    private void readExistingProperties() {
        try (Stream<String> lines = Files.lines(Paths.get(filename), Charset.defaultCharset())) {
            existingProperties = lines.collect(Collectors.toList());
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    private void checkForMissingPropertiesInFile() {
        for (String key : serviceProperties.keySet()) {
            if (existingProperties.stream().noneMatch(s -> s.contains(key))) {
                logger.warn("{} is missing in {}", key, filename);
            }
        }
    }

    private void checkForUnsetPropertiesInFile() {
        existingProperties.stream()
                .filter(prop -> prop.endsWith("="))
                .forEach(prop -> logger.warn("{} is not set {}", prop, pid));
    }

    private void checkForDeprecatedProperties() {
        for (String existingProp : existingProperties) {
            if (!existingProp.contains("#") && !existingProp.isEmpty()
                    && serviceProperties.keySet().stream().noneMatch(key -> existingProp.contains(key))) {
                logger.warn("{} in {} is deprecated", existingProp, filename);
            }
        }
    }
}
