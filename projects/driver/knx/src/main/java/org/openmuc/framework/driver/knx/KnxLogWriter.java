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
package org.openmuc.framework.driver.knx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tuwien.auto.calimero.log.LogLevel;
import tuwien.auto.calimero.log.LogWriter;

public class KnxLogWriter extends LogWriter {

    private static Logger logger = LoggerFactory.getLogger(KnxLogWriter.class);

    /*
     * (non-Javadoc)
     * 
     * @see tuwien.auto.calimero.log.LogWriter#write(java.lang.String, tuwien.auto.calimero.log.LogLevel,
     * java.lang.String)
     */
    @Override
    public void write(String logService, LogLevel level, String msg) {
        write(logService, level, msg, null);
    }

    /*
     * (non-Javadoc)
     * 
     * @see tuwien.auto.calimero.log.LogWriter#write(java.lang.String, tuwien.auto.calimero.log.LogLevel,
     * java.lang.String, java.lang.Throwable)
     */
    @Override
    public void write(String logService, LogLevel level, String msg, Throwable t) {
        String logMsg = logService + " - " + msg;
        // Logger logger = LoggerFactory.getLogger(logService);
        if (level.equals(LogLevel.TRACE)) {
            logger.trace(logMsg);
        }
        else if (level.equals(LogLevel.INFO)) {
            logger.debug(logMsg);
        }
        else if (level.equals(LogLevel.WARN)) {
            logger.info(logMsg);
        }
        else if (level.equals(LogLevel.ERROR)) {
            logger.warn(logMsg);
        }
        else if (level.equals(LogLevel.FATAL)) {
            logger.error(logMsg);
        }
        else {
            logger.debug(level.toString().toUpperCase() + " " + logMsg);
        }

        if (logger.isTraceEnabled() && t != null) {
            logger.warn(t.getMessage());
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see tuwien.auto.calimero.log.LogWriter#flush()
     */
    @Override
    public void flush() {
    }

    /*
     * (non-Javadoc)
     * 
     * @see tuwien.auto.calimero.log.LogWriter#close()
     */
    @Override
    public void close() {
    }

}
