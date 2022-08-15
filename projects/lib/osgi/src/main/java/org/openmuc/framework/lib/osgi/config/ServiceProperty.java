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

/**
 * Enriches the classical property (key=value) with meta data. A list of ServiceProperties can be managed by a Settings
 * class extending {@link GenericSettings}
 */
public class ServiceProperty {

    private String key;
    private String description;
    private String defaultValue;
    private final boolean mandatory;
    private String value;

    public ServiceProperty(String key, String description, String defaultValue, boolean mandatory) {
        setKey(key);
        setDescription(description);
        setDefaultValue(defaultValue);
        this.mandatory = mandatory;
        this.value = this.defaultValue;
    }

    public void update(String value) {
        if (value == null) {
            // avoid later null checks
            this.value = "";
        }
        else {
            this.value = value;
        }
    }

    public String getKey() {
        return key;
    }

    public String getDescription() {
        return description;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public boolean isMandatory() {
        return mandatory;
    }

    public String getValue() {
        return value;
    }

    private void setKey(String key) {
        if (key == null || key.isEmpty()) {
            // key is important - therefor raise exception
            throw new IllegalArgumentException("key must not be null or empty!");
        }
        else {
            this.key = key;
        }
    }

    private void setDescription(String description) {
        if (description == null) {
            // description is optional, don't raise exception here, but change it to empty string
            // to avoid countless "null" checks later in classes using this.
            this.description = "";
        }
        else {
            this.description = description;
        }
    }

    private void setDefaultValue(String defaultValue) {
        if (defaultValue == null) {
            // defaultValue is optional, don't raise exception here, but change it to empty string
            // to avoid countless "null" checks later in classes using this.
            this.defaultValue = "";
        }
        else {
            this.defaultValue = defaultValue;
        }
    }

    @Override
    public String toString() {
        String optional = "# ";
        if (!mandatory) {
            optional += "(Optional) ";
        }
        return optional + description + "\n" + key + "=" + defaultValue + "\n";
    }

}
