/*
 * Copyright 2011-14 Fraunhofer ISE
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
package org.openmuc.framework.server.restws.json;

public class JsonFieldInformation {

    int level;
    String fieldName;
    JsonTextType type;

    JsonFieldInformation() {

    }

    JsonFieldInformation(int level, String fieldName, JsonTextType type) {
        this.level = level;
        /**
         * TODO handle case if json text is null, false, true
         */
        this.fieldName = fieldName;
        this.type = type;
    }

    public int getFieldLevel() {
        return this.level;
    }

    public String getFieldName() {
        return this.fieldName;
    }

    public JsonTextType getFieldJsonTextType() {
        return this.type;
    }

    public String getFieldJsonTextTypeString() {
        return this.type.toString();
    }

    @Override
    public String toString() {
        return "(" + this.level + "," + this.fieldName + "," + this.type + ")";
    }

}
