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

package org.openmuc.framework.webui.spi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import org.osgi.framework.BundleContext;

/**
 * Helper class to load resources from the bundle classpath
 * 
 */
public final class ResourceLoader {

    private final BundleContext context;

    public ResourceLoader(BundleContext context) {
        this.context = context;
    }

    /**
     * 
     * Get resource as URL
     * 
     * @param name
     *            Resource name relative to bundles classpath
     * @return URL object of the requested resource.
     */
    public URL getResource(String name) {
        return context.getBundle().getResource(name);
    }

    /**
     * 
     * Get a text file resource as String
     * 
     * @param name
     *            Resource name relative to bundles classpath
     * @return String representation of the requested resource. null if resource is not available.
     */
    public String getResourceAsString(String name) {
        URL url = getResource(name);

        try {
            InputStream stream = (InputStream) url.getContent();

            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

            String line = null;
            StringBuilder builder = new StringBuilder();

            while ((line = reader.readLine()) != null) {
                builder.append(line);
                builder.append('\n');
            }

            reader.close();

            return builder.toString();
        } catch (IOException e) {
            return null;
        }

    }

}
