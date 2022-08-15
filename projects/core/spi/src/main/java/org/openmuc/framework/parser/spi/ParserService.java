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

package org.openmuc.framework.parser.spi;

import java.util.List;

import org.openmuc.framework.data.Record;
import org.openmuc.framework.data.ValueType;
import org.openmuc.framework.datalogger.spi.LoggingRecord;

/**
 * The ParserService provides methods to serialize and deserialize OpenMUC records.
 */
public interface ParserService {

    /**
     * Serializes a given java datatype to byte array. The needed datatype depends on the concrete implementation of
     * this service.
     *
     * @param openMucRecord
     *            logging record for serializing
     * @return serialized record as byte array
     * @throws SerializationException
     *             when something goes wrong while serializing
     */
    byte[] serialize(LoggingRecord openMucRecord) throws SerializationException;

    /**
     * Serializes a list of LogRecordContainers.
     *
     * @param openMucRecords
     *            list of logging records for serializing
     * @return serialized records as byte array
     * @throws SerializationException
     *             when something goes wrong while serializing
     */
    byte[] serialize(List<LoggingRecord> openMucRecords) throws SerializationException;

    /**
     * Deserializes a given JSON-String as byte array to {@link org.openmuc.framework.data.Record}. The format of the
     * byte array depends on the concrete implementation of this service.
     *
     * @param byteArray
     *            received JSON-String
     * @param valueType
     *            defines the type of the value, which is encapsulated in the received JSON-String
     *            {@link org.openmuc.framework.data.ValueType}
     * @return deserialized instance of {@link org.openmuc.framework.data.Record}
     */
    Record deserialize(byte[] byteArray, ValueType valueType);
}
