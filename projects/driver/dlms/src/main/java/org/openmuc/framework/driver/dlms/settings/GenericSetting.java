package org.openmuc.framework.driver.dlms.settings;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.DatatypeConverter;

import org.openmuc.framework.config.ArgumentSyntaxException;

public abstract class GenericSetting {

    private static final String SEPARATOR = ";";
    private static final String PAIR_SEP = "=";

    public static <T extends GenericSetting> String strSyntaxFor(Class<T> settings) {
        StringBuilder sbOptinal = new StringBuilder();

        StringBuilder sb = new StringBuilder().append("SYNOPSIS: ");
        boolean first = true;
        for (Field field : settings.getDeclaredFields()) {

            Option option = field.getAnnotation(Option.class);
            if (option == null) {
                continue;
            }

            String str = strFor(option, first);
            sbOptinal.append(str);
            first = false;
        }
        sb.append(sbOptinal);

        return sb.toString().trim();
    }

    private static String strFor(Option option, boolean first) {
        StringBuilder sb = new StringBuilder();

        String value = option.value();
        boolean mandatory = option.mandatory();
        if (!mandatory) {
            sb.append("[");
        }

        if (!first) {
            sb.append(SEPARATOR);
        }

        String range;
        if (option.range().isEmpty()) {
            range = option.value();
        }
        else {
            range = option.range();
        }

        sb.append(MessageFormat.format("{0}{1}<{2}>", value, PAIR_SEP, range));

        if (!mandatory) {
            sb.append("]");
        }

        return sb.append(" ").toString();

    }

    protected int parseFields(String settingsStr) throws ArgumentSyntaxException {
        if (settingsStr.trim().isEmpty()) {
            return 0;
        }
        Class<? extends GenericSetting> settingsClass = this.getClass();
        Map<String, String> setting = toMap(settingsStr);

        int setFieldCounter = 0;
        for (Field field : settingsClass.getDeclaredFields()) {

            Option option = field.getAnnotation(Option.class);
            if (option == null) {
                continue;
            }

            String val = setting.get(option.value());

            if (val != null) {
                try {
                    setField(field, val, option);
                    ++setFieldCounter;
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (NoSuchFieldException e) {
                    e.printStackTrace();
                }
            }
            else if (option.mandatory()) {
                String message = MessageFormat.format("Mandatory parameter {0} is nor present in {1}.", option.value(),
                        this.getClass().getSimpleName());
                throw new ArgumentSyntaxException(message);
            }

        }
        return setFieldCounter;
    }

    private Map<String, String> toMap(String settingsStr) throws ArgumentSyntaxException {
        String[] settings = settingsStr.trim().split(SEPARATOR);
        Map<String, String> settingMap = new HashMap<>(settings.length);
        for (String setting : settings) {
            String[] s = setting.split(PAIR_SEP);

            if (s.length != 2) {
                String message = MessageFormat.format("Illegal setting ''{0}''.", setting);
                throw new ArgumentSyntaxException(message);
            }

            String key = s[0].trim();
            if (settingMap.put(key, s[1].trim()) != null) {
                String message = MessageFormat.format("''{0}'' has been set twice.", key);
                throw new ArgumentSyntaxException(message);
            }
        }
        return settingMap;
    }

    private void setField(Field field, String value, Option option)
            throws IllegalAccessException, NoSuchFieldException, ArgumentSyntaxException {

        Object newVal = extracted(field, value);
        field.set(this, newVal);
    }

    private Object extracted(Field field, String value)
            throws ArgumentSyntaxException, IllegalAccessException, NoSuchFieldException {

        String trimmed = value.trim();
        field.setAccessible(true);

        Class<?> type = field.getType();

        if (type.isAssignableFrom(boolean.class) || type.isAssignableFrom(Boolean.class)) {
            return extractBoolean(trimmed);
        }
        else if (type.isAssignableFrom(byte.class) || type.isAssignableFrom(Byte.class)) {
            return extractByte(trimmed);
        }
        else if (type.isAssignableFrom(short.class) || type.isAssignableFrom(Short.class)) {
            return extractShort(trimmed);
        }
        else if (type.isAssignableFrom(int.class) || type.isAssignableFrom(Integer.class)) {
            return extractInteger(trimmed);
        }
        else if (type.isAssignableFrom(long.class) || type.isAssignableFrom(Long.class)) {
            return extractLong(trimmed);
        }
        else if (type.isAssignableFrom(float.class) || type.isAssignableFrom(Float.class)) {
            return extractFloat(trimmed);
        }
        else if (type.isAssignableFrom(double.class) || type.isAssignableFrom(Double.class)) {
            return extractDouble(trimmed);
        }
        else if (type.isAssignableFrom(String.class)) {
            return value;
        }
        else if (type.isAssignableFrom(byte[].class)) {
            return extractByteArray(trimmed);
        }
        else if (type.isAssignableFrom(InetAddress.class)) {
            return extractInetAddress(trimmed);
        }
        else {
            throw new NoSuchFieldException(
                    type + "  Driver implementation error not supported data type. Report driver developer\n");
        }
    }

    private boolean extractBoolean(String value) throws ArgumentSyntaxException {
        try {
            return Boolean.parseBoolean(value);
        } catch (NumberFormatException e) {
            throw argumentSyntaxException(boolean.class.getSimpleName());
        }
    }

    private byte extractByte(String value) throws ArgumentSyntaxException {
        try {
            return parseNumber(value).byteValue();
        } catch (NumberFormatException | ParseException e) {
            throw argumentSyntaxException(Short.class.getSimpleName());
        }
    }

    private short extractShort(String value) throws ArgumentSyntaxException {
        try {
            return parseNumber(value).shortValue();
        } catch (NumberFormatException | ParseException e) {
            throw argumentSyntaxException(Short.class.getSimpleName());
        }
    }

    private int extractInteger(String value) throws ArgumentSyntaxException {
        try {
            return parseNumber(value).intValue();
        } catch (NumberFormatException | ParseException e) {
            throw argumentSyntaxException(Integer.class.getSimpleName());
        }
    }

    private long extractLong(String value) throws ArgumentSyntaxException {
        try {
            return parseNumber(value).longValue();
        } catch (NumberFormatException | ParseException e) {
            throw argumentSyntaxException(Long.class.getSimpleName());
        }
    }

    private float extractFloat(String value) throws ArgumentSyntaxException {
        try {
            return parseNumber(value).floatValue();
        } catch (NumberFormatException | ParseException e) {
            throw argumentSyntaxException(Float.class.getSimpleName());
        }
    }

    private double extractDouble(String value) throws ArgumentSyntaxException {
        try {
            return parseNumber(value).doubleValue();
        } catch (NumberFormatException | ParseException e) {
            throw argumentSyntaxException(Double.class.getSimpleName());
        }
    }

    private byte[] extractByteArray(String value) throws ArgumentSyntaxException {
        if (!value.startsWith("0x")) {
            return value.getBytes(StandardCharsets.US_ASCII);
        }

        try {
            return DatatypeConverter.parseHexBinary(value.substring(2).trim());
        } catch (IllegalArgumentException e) {
            throw argumentSyntaxException(byte[].class.getSimpleName());
        }

    }

    private InetAddress extractInetAddress(String value) throws ArgumentSyntaxException {
        try {
            return InetAddress.getByName(value);
        } catch (UnknownHostException e) {
            throw argumentSyntaxException(InetAddress.class.getSimpleName());
        }
    }

    private ArgumentSyntaxException argumentSyntaxException(String returnType) {
        return new ArgumentSyntaxException(MessageFormat.format("Value of {0} in {1} is not type of {2}.", "error",
                this.getClass().getSimpleName(), returnType));
    }

    private static Number parseNumber(String value) throws ParseException {
        return NumberFormat.getNumberInstance().parse(value);
    }

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface Option {

        String value();

        boolean mandatory()

        default false;

        String range() default "";
    }

}
