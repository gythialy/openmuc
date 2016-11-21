/*
 * Copyright 2011-16 Fraunhofer ISE
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
package org.openmuc.framework.config;

public class DriverInfo {

    private final String id;
    private final String description;
    private final String deviceAddressSyntax;
    private final String settingsSyntax;
    private final String channelAddressSyntax;
    private final String deviceScanSettingsSyntax;

    /**
     * Constructor to set driver info
     * 
     * @param id
     *            driver ID
     * @param description
     *            driver description
     * @param deviceAddressSyntax
     *            device address syntax
     * @param settingsSyntax
     *            device settings syntax
     * @param channelAddressSyntax
     *            channel address syntax
     * @param deviceScanSettingsSyntax
     *            device scan settings syntax
     */
    public DriverInfo(String id, String description, String deviceAddressSyntax, String settingsSyntax,
            String channelAddressSyntax, String deviceScanSettingsSyntax) {
        this.id = id;
        this.description = description;
        this.deviceAddressSyntax = deviceAddressSyntax;
        this.settingsSyntax = settingsSyntax;
        this.channelAddressSyntax = channelAddressSyntax;
        this.deviceScanSettingsSyntax = deviceScanSettingsSyntax;
    }

    /**
     * Returns the ID of the driver. The ID may only contain ASCII letters, digits, hyphens and underscores. By
     * convention the ID should be meaningful and all lower case letters (e.g. "mbus", "modbus").
     * 
     * @return the unique ID of the driver.
     */
    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public String getDeviceAddressSyntax() {
        return deviceAddressSyntax;
    }

    public String getSettingsSyntax() {
        return settingsSyntax;
    }

    public String getChannelAddressSyntax() {
        return channelAddressSyntax;
    }

    public String getDeviceScanSettingsSyntax() {
        return deviceScanSettingsSyntax;
    }

}
