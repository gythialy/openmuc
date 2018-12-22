/*
 * Copyright 2011-18 Fraunhofer ISE
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
package org.openmuc.framework.lib.json.rest.objects;

import org.openmuc.framework.config.DriverConfig;
import org.openmuc.framework.config.IdCollisionException;
import org.openmuc.framework.lib.json.exceptions.RestConfigIsNotCorrectException;

public class RestDriverConfigMapper {

    public static RestDriverConfig getRestDriverConfig(DriverConfig dc) {

        RestDriverConfig rdc = new RestDriverConfig();
        rdc.setId(dc.getId());
        rdc.setConnectRetryInterval(dc.getConnectRetryInterval());
        rdc.setDisabled(dc.isDisabled());
        rdc.setSamplingTimeout(dc.getSamplingTimeout());
        return rdc;
    }

    public static void setDriverConfig(DriverConfig dc, RestDriverConfig rdc, String idFromUrl)
            throws IdCollisionException, RestConfigIsNotCorrectException {

        if (dc == null) {
            throw new RestConfigIsNotCorrectException("DriverConfig is null!");
        }
        else {
            if (rdc != null) {
                if (rdc.getId() != null && !rdc.getId().equals("") && !idFromUrl.equals(rdc.getId())) {
                    dc.setId(rdc.getId());
                }
                dc.setConnectRetryInterval(rdc.getConnectRetryInterval());
                dc.setDisabled(rdc.isDisabled());
                dc.setSamplingTimeout(rdc.getSamplingTimeout());
            }
            else {
                throw new RestConfigIsNotCorrectException();
            }
        }

    }
}
