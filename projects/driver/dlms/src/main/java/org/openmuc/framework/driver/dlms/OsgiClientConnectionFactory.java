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
package org.openmuc.framework.driver.dlms;

import java.util.ArrayList;
import java.util.List;

import org.openmuc.jdlms.client.ClientConnectionFactory;
import org.openmuc.jdlms.client.ClientConnectionSettings;
import org.openmuc.jdlms.client.ILowerLayerFactory;
import org.openmuc.jdlms.client.hdlc.HdlcClientLayerFactory;
import org.openmuc.jdlms.client.ip.TcpClientLayerFactory;
import org.openmuc.jdlms.client.ip.UdpClientLayerFactory;

/**
 * Implementation of {@link ILowerLayerFactory} statically loading all available LowerLayerFactories.
 * 
 * @author Karsten Mueller-Bier
 */
public final class OsgiClientConnectionFactory extends ClientConnectionFactory {

    private final List<ILowerLayerFactory> factories;

    public OsgiClientConnectionFactory() {
        factories = new ArrayList<>(3);
        factories.add(new HdlcClientLayerFactory());
        factories.add(new UdpClientLayerFactory());
        factories.add(new TcpClientLayerFactory());
    }

    @Override
    protected ILowerLayerFactory getLowerLayerFactory(
            @SuppressWarnings("rawtypes") Class<? extends ClientConnectionSettings> settingsClass) {
        for (ILowerLayerFactory factory : factories) {
            if (factory.accepts(settingsClass)) {
                return factory;
            }
        }

        return null;
    }
}
