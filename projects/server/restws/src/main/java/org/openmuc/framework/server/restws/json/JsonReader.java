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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.ListIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JsonReader {
    private final static Logger logger = LoggerFactory.getLogger(JsonReader.class);
    static String text;
    static String tmpText;
    static LinkedHashMap<JsonFieldInformation, Integer> tmpMap = new LinkedHashMap<JsonFieldInformation, Integer>();

    private static JsonObjectMap FieldValueReader() {

        JsonObjectMap finalMap = new JsonObjectMap();
        Pattern FieldValueStringPattern = Pattern.compile("((\\[)|(:)|(,))\".*?\"((\\])|(})|(,))");
        Pattern FieldValueObjetPattern = Pattern.compile("((\\[)|(:)|(,))\\{.*?\\}((\\])|(})|(,))");
        Pattern FieldValueArrayPattern = Pattern.compile("((\\[)|(:)|(,))\\[.*?\\]((\\])|(})|(,))");
        Pattern FieldValueNumberPattern = Pattern
                .compile(
                        "((\\[)|(:)|(,))(((-)|(\\+)){0,1})([0-9]{0,})([.eE]{0,1})([0-9]+)((\\])|(})|(,))");
        Matcher matcher;

        ArrayList<JsonFieldInformation> arrayList = new ArrayList<JsonFieldInformation>(tmpMap.keySet());
        ListIterator<JsonFieldInformation> iter = arrayList.listIterator();
        while (iter.hasNext()) {
            iter.next();
        }

        while (iter.hasPrevious()) {
            JsonFieldInformation jfi = iter.previous();
            int beginIndex = tmpMap.get(jfi);
            if (jfi.getFieldJsonTextType() == JsonTextType.JsonString) {
                matcher = FieldValueStringPattern.matcher(text.substring(beginIndex - 1));
                if (matcher.find()) {
                    String tmpValue = matcher.group(0);
                    tmpValue = tmpValue.substring(1, tmpValue.length() - 1);
                    finalMap.put(jfi, tmpValue);
                    // System.out.println(tmpValue);
                    tmpText = tmpText.replace(tmpValue, "");
                    logger.info(tmpText);
                } else {
                    logger.info("FieldValueReader: 1");
                    return null;
                }
            } else if (jfi.getFieldJsonTextType() == JsonTextType.JsonObject) {
                matcher = FieldValueObjetPattern.matcher(text.substring(beginIndex - 1));
                if (matcher.find()) {
                    String tmpValue = matcher.group(0);
                    tmpValue = tmpValue.substring(1, tmpValue.length() - 1);
                    // System.out.println(tmpValue);
                    finalMap.put(jfi, "-OBJ-");
                } else {
                    logger.info("FieldValueReader: 2");
                    return null;
                }
            } else if (jfi.getFieldJsonTextType() == JsonTextType.JsonNumber) {
                matcher = FieldValueNumberPattern.matcher(text.substring(beginIndex - 1));
                if (matcher.find()) {
                    String tmpValue = matcher.group(0);
                    tmpValue = tmpValue.substring(1, tmpValue.length() - 1);
                    // System.out.println(tmpValue);
                    finalMap.put(jfi, tmpValue);
                    tmpText = tmpText.replace(tmpValue, "");
                    logger.info(tmpText);
                } else {
                    logger.info("FieldValueReader: 3");
                    return null;
                }
            } else if (jfi.getFieldJsonTextType() == JsonTextType.JsonNull) {
                String expectedNull = text.substring(beginIndex - 1, beginIndex + 4);
                expectedNull = expectedNull.substring(1);
                if (expectedNull.equals("null")) {
                    finalMap.put(jfi, "null");
                    tmpText = tmpText.replace(expectedNull, "");
                    logger.info(tmpText);
                } else {
                    System.out.println("NOJSON");
                    return null;
                }
            } else if (jfi.getFieldJsonTextType() == JsonTextType.JsonTrue) {
                String expectedTrue = text.substring(beginIndex - 1, beginIndex + 4);
                expectedTrue = expectedTrue.substring(1);
                if (expectedTrue.equals("true")) {
                    finalMap.put(jfi, "true");
                    tmpText = tmpText.replace(expectedTrue, "");
                    logger.info(tmpText);
                } else {
                    System.out.println("NOJSON");
                    return null;
                }
            } else if (jfi.getFieldJsonTextType() == JsonTextType.JsonFalse) {
                String expectedFalse = text.substring(beginIndex - 1, beginIndex + 5);
                expectedFalse = expectedFalse.substring(1);
                if (expectedFalse.equals("false")) {
                    finalMap.put(jfi, "false");
                    tmpText = tmpText.replace(expectedFalse, "");
                    logger.info(tmpText);
                } else {
                    System.out.println("NOJSON");
                    return null;
                }
            } else if (jfi.getFieldJsonTextType() == JsonTextType.JsonArray) {
                matcher = FieldValueArrayPattern.matcher(text.substring(beginIndex - 1));
                if (matcher.find()) {
                    String tmpValue = matcher.group(0);
                    tmpValue = tmpValue.substring(1, tmpValue.length() - 1);
                    // System.out.println(tmpValue);
                    finalMap.put(jfi, tmpValue);
                    tmpText = tmpText.replace(tmpValue, "");
                    logger.info(tmpText);
                } else {
                    System.out.println("NOJSON");
                    return null;
                }
            } else {
                logger.info("FieldValueReader: no Type");
                return null;

            }

        }
        return finalMap;

    }

    private static LinkedHashMap<JsonFieldInformation, Integer> FieldNameReader() {
        Pattern FieldNamePattern = Pattern.compile("((\\[)|(\\{)|(,))\"[a-zA-Z_0-9.-]*?\":");
        Matcher matcher;
        Boolean found = false;
        LinkedHashMap<JsonFieldInformation, Integer> tmpMap = new LinkedHashMap<JsonFieldInformation, Integer>();

        while ((matcher = FieldNamePattern.matcher(tmpText)).find()) {
            found = true;
            String foundField = matcher.group(0).substring(1);
            // Level
            int index = text.indexOf(foundField);
            int endIndex = index + foundField.length();
            // System.out.println("Index of: " + foundField + " is: " + index);
            int level = levelToIndex(text, index);
            if (level <= 0) {
                logger.info("FieldValueReader: level is 0 or less");
                return null;
            }
            // Type
            char next = text.charAt(endIndex);
            JsonTextType type = expectedType(next);
            // System.out.println("Handing over next char: " + next);
            if (type == null) {
                logger.info("FieldValueReader: type is null");
                return null;
            }
            // System.out.println("Expecting type: " + expectedType(next));
            // foundField
            tmpText = tmpText.replace(foundField, "");
            logger.info(tmpText);

            foundField = foundField.replace("\"", "").replace(":", "");

            JsonFieldInformation jfi = new JsonFieldInformation(level,
                                                                foundField,
                                                                expectedType(next));

            tmpMap.put(jfi, endIndex);

            // System.out.println(tmpText);

        }
        if (!found) {
            logger.info("FieldValueReader: found is false");
            return null;
        }
        return tmpMap;

    }

    public static JsonObjectMap JsonStreamToMap(String jsontext) {

        text = cleanLine(jsontext);
        tmpText = text;
        logger.info("this is text: " + text);
        logger.info("this is tmptext: " + tmpText);

        tmpMap = FieldNameReader();
        if (tmpMap == null) {
            logger.info("FieldNameReader is null");
            return null;
        }
        JsonObjectMap fMap = FieldValueReader();
        if (fMap == null) {
            logger.info("FieldValueReader is null");
            return null;
        }
        if (!isValidJsonStructure(tmpText)) {
            logger.info("is not valid json structur: " + tmpText);
            return null;
        }
        return fMap;

    }

    private static String cleanLine(String line) {
        line = line.replace(" ", "").replace("\t", "").replace("\n", "").replace("\r", "");
        return line;
    }

    private static int levelToIndex(String line, int index) {
        line = line.substring(0, index);
        return determineNbrTab(line);

    }

    private static int determineNbrTab(String text) {
        int nbrTabs = 0;
        nbrTabs = text.length() - text.replace("{", "").length();
        nbrTabs += text.length() - text.replace("[", "").length();
        nbrTabs -= text.length() - text.replace("}", "").length();
        nbrTabs -= text.length() - text.replace("]", "").length();

        return nbrTabs;
    }

    private static JsonTextType expectedType(char next) {
        switch (next) {
        case '\"':
            return JsonTextType.JsonString;
        case '{':
            return JsonTextType.JsonObject;
        case '[':
            return JsonTextType.JsonArray;
        case 'f':
            return JsonTextType.JsonFalse;
        case 't':
            return JsonTextType.JsonTrue;
        case 'n':
            return JsonTextType.JsonNull;
        default:
            if (Character.isDigit(next)) {
                return JsonTextType.JsonNumber;
            } else {
                return null;
            }
        }
    }

    private static Boolean isValidJsonStructure(String txt) {
        if (!txt.matches("[{},]+")) {
            return false;
        }
        char firstChar = txt.charAt(0);
        char lastChar = txt.charAt(txt.length() - 1);
        if (firstChar == ',' || lastChar == ',') {
            return false;
        }

        String tmp = txt.replace(",", "");
        if (determineNbrTab(txt) != 0) {
            return false;
        }
        String tmp2 = tmp;
        int i = 0;
        int nbrTabs = tmp.length() / 2;
        int nbrOpen = 0;
        int nbrClos = 0;
        do {
            i++;
            tmp2 = tmp.substring(0, i);
            nbrOpen = tmp2.length() - tmp2.replace("{", "").length();
            nbrClos = tmp2.length() - tmp2.replace("}", "").length();
        } while ((nbrOpen != nbrClos) && (nbrTabs * 2 != i));
        if (nbrOpen != nbrTabs || nbrClos != nbrTabs) {
            return false;
        }
        if (nbrTabs * 2 != i) {
            return false;
        }
        return true;
    }

}
