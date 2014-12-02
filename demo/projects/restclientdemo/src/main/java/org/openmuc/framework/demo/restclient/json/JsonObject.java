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

public class JsonObject extends JsonText {

    public JsonTextType jsonType;

    public JsonObject() {
        super();
        this.jsonType = JsonTextType.JsonObject;
    }

    @Override
    public void writeStartObject() {
        int numberOfTabs = 0, i;
        numberOfTabs = determineNbrTab();
        for (i = 0; i < numberOfTabs; ++i) {
            this.jsonText += "\t";
        }
        this.jsonText += "{\n";
    }

    @Override
    public void writeEndObject() {

        if (this.jsonText.endsWith(",\n")) {
            this.jsonText = this.jsonText.substring(0, this.jsonText.length() - 2) + "\n";
        }

        int numberOfTabs = 0, i;
        numberOfTabs = determineNbrTab();

        for (i = 1; i < numberOfTabs; ++i) {
            this.jsonText += "\t";
        }
        this.jsonText += "}";
        if (numberOfTabs > 1) {
            this.jsonText += "\n";
        }
    }

    @Override
    public void writeObjectField(String field) {
        int numberOfTabs = 0, i;
        numberOfTabs = determineNbrTab();

        for (i = 0; i < numberOfTabs; ++i) {
            this.jsonText += "\t";
        }
        this.jsonText += "\"" + field + "\"" + " : ";
    }

    @Override
    public void writeObjectValue(String value) {
        this.jsonText += "\"" + value + "\"" + ",\n";
    }

    @Override
    public void writeObjectValue(String value, boolean isLastValue) {
        this.writeObjectValue(value);
        if (isLastValue) {

            this.jsonText.substring(0, this.jsonText.lastIndexOf(",") - 1);
            appendJsonText("\n");

        }
    }

    @Override
    public void writeObjectMember(String memberName) {
        int numberOfTabs = 0, i;
        numberOfTabs = determineNbrTab();
        for (i = 0; i < numberOfTabs; ++i) {
            this.jsonText += "\t";
        }
        this.jsonText += "\"" + memberName + "\"" + " : {\n";

    }

    @Override
    public void writeObjectValue(long value) {
        this.jsonText += Long.toString(value) + ",\n";
    }

    public void writeObjectValue(double value) {
        this.jsonText += Double.toString(value) + ",\n";
    }

    @Override
    public void writeStartArray() {
        // TODO Auto-generated method stub

    }

    @Override
    public void writeEndArray() {
        // TODO Auto-generated method stub

    }

    @Override
    public void writeArrayValue(String value) {
        // TODO Auto-generated method stub

    }

    @Override
    public void writeArrayValue(JsonObject jObj) {
        // TODO Auto-generated method stub

    }

    @Override
    public void writeObjectValue(long value, boolean isLastValue) {
        this.writeObjectValue(value);
        if (isLastValue) {
            this.jsonText.substring(0, this.jsonText.lastIndexOf(",") - 1);
            appendJsonText("\n");

        }
    }

}
