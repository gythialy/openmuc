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
package org.openmuc.framework.demo.restclient.json;

public class JsonArray extends JsonText implements JsonWriter {

    public JsonTextType jsonType;

    public JsonArray() {
        super();
        this.jsonType = JsonTextType.JsonArray;
    }

    @Override
    public void writeStartArray() {
        this.jsonText += "[\n";
    }

    @Override
    public void writeEndArray() {
        if (this.jsonText.endsWith(",\n")) {
            this.jsonText = this.jsonText.substring(0, this.jsonText.length() - 2) + "\n";
        }
        this.jsonText += "]";
    }

    @Override
    public void writeArrayValue(String value) {
        this.jsonText += "\t\"" + value + "\",\n";
    }

    @Override
    public void writeArrayValue(JsonObject jObj) {
        int numberOfTabs = 0, i;
        numberOfTabs = determineNbrTab();
        for (i = 0; i < numberOfTabs; ++i) {
            jObj.setJsonText(jObj.getJsonText().replace("\n", "\n\t"));
        }

        this.jsonText += "\t" + jObj.jsonText + ",\n";
    }

    @Override
    public void writeStartObject() {
        // TODO Auto-generated method stub

    }

    @Override
    public void writeEndObject() {
        // TODO Auto-generated method stub

    }

    @Override
    public void writeObjectField(String field) {
        // TODO Auto-generated method stub

    }

    @Override
    public void writeObjectValue(String value) {
        // TODO Auto-generated method stub

    }

    @Override
    public void writeObjectValue(String value, boolean isLastValue) {
        // TODO Auto-generated method stub

    }

    @Override
    public void writeObjectMember(String memberName) {
        // TODO Auto-generated method stub

    }

    @Override
    public void writeObjectValue(long value) {
        // TODO Auto-generated method stub

    }

    @Override
    public void writeObjectValue(long value, boolean isLastValue) {
        // TODO Auto-generated method stub

    }

}
