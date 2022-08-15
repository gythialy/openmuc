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

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Intention of this class is to provide a "Special Case Object" for invalid dictionaries. See
 * {@link #getCleanedUpDeepCopyOfDictionary()}
 */
public class DictionaryPreprocessor {

    private static final Logger logger = LoggerFactory.getLogger(DictionaryPreprocessor.class);
    private Dictionary<String, String> dictionary;
    private boolean osgiInit;

    public DictionaryPreprocessor(Dictionary<String, ?> newDictionary) {

        // call this first before to print original dictionary passed by MangedService updated()
        logDebugPrintDictionary(newDictionary);

        osgiInit = false;
        if (newDictionary == null || newDictionary.isEmpty()) {
            this.dictionary = new Hashtable<>();
        }
        else if (!newDictionary.isEmpty()) {
            // create deep copy to not manipulate the original dictionary
            Dictionary<String, String> tempDict = getDeepCopy(newDictionary);

            // clean up dictionary - remove "osgi" framework related keys which may be inside the dictionary
            // given to the updated() method of ManagedService implementation. These entries can be ignored,
            // since they are not part of the actual configuration. Removing them here safes condition checks later.
            tempDict.remove("service.pid");
            tempDict.remove("felix.fileinstall.filename");
            this.dictionary = tempDict;
        }

        if (this.dictionary.isEmpty()) {
            // either it was null or empty before or by removing service.pid it became empty
            osgiInit = true;
        }
    }

    /**
     * @return <b>true</b> when it was a intermediate updated() call (MangedService) during starting the OSGi framework.
     *         During start the updated() is called with an dictionary = null or with dictionary which has only one
     *         entry with service.pid. With this flag you can ignore such calls.
     */
    public boolean wasIntermediateOsgiInitCall() {
        return osgiInit;
    }

    /**
     * @return a cleaned up, deep copy of dictionary which is not null. It is at least an empty dictionary. NOTE values
     *         to a key might be null)
     */
    public Dictionary<String, String> getCleanedUpDeepCopyOfDictionary() {
        return dictionary;
    }

    private Dictionary<String, String> getDeepCopy(Dictionary<String, ?> propertyDict) {
        Dictionary<String, String> propertiesCopy = new Hashtable<>();
        Enumeration<String> keys = propertyDict.keys();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            propertiesCopy.put(key, (String) propertyDict.get(key));
        }
        return propertiesCopy;
    }

    /**
     * Method for debugging purposes to print whole dictionary
     * <p>
     * If the key contains "password", "*****" is shown instead of the corresponding value (which would be the
     * password).
     *
     * @param propertyDict
     */
    private void logDebugPrintDictionary(Dictionary<String, ?> propertyDict) {
        if (logger.isDebugEnabled()) {
            if (propertyDict != null) {
                StringBuilder sb = new StringBuilder();
                Enumeration<String> keys = propertyDict.keys();
                while (keys.hasMoreElements()) {
                    String key = keys.nextElement();
                    String dictValue = (String) propertyDict.get(key);
                    if (dictValue != null) {
                        if (key != null && key.contains("password")) {
                            sb.append(key + "=*****\n");
                        }
                        else {
                            sb.append(key + "=" + dictValue + "\n");
                        }
                    }
                    else {
                        sb.append(key + "=null" + "\n");
                    }
                }
                logger.debug("Dictionary given by ManagedService updated(): \n{}", sb.toString());
            }
            else {
                logger.debug("Dictionary given by ManagedService updated(): is null");
            }
        }
    }

}
