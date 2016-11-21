package org.openmuc.framework.driver.iec60870.settings;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.MessageFormat;
import java.util.Locale;

import org.openmuc.framework.config.ArgumentSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class GenericSetting {

    private final static String SEPARATOR = ";";
    private final static String PAIR_SEP = "=";
    private final static String PREFIX = "prefix";
    private final static String TYPE = "type";
    private final static String MANDATORY = "mandatory";
    private final static Locale LOCALE = Locale.ENGLISH;

    private final static Logger logger = LoggerFactory.getLogger(GenericSetting.class);

    @SuppressWarnings("unchecked")
    public static String syntax(Class<? extends GenericSetting> genericSettings) {
        Class<Enum<? extends OptionI>> options = (Class<Enum<? extends OptionI>>) genericSettings
                .getDeclaredClasses()[0];
        // Class<Enum<? extends OptionI>> options = null;
        // for (Class<?> c : genericSettings.getClasses()) {
        // if (c.isEnum() && OptionI.class.isAssignableFrom(c)
        // && !c.getEnclosingClass().equals(GenericSetting.class)) {
        // options = (Class<Enum<? extends OptionI>>) c;
        // }
        // }

        StringBuilder sb = new StringBuilder();
        StringBuilder sbNotMandetory = new StringBuilder();

        if (options == null) {
            String errorMessage = "Driver implementation error, in method syntax(). Could not find class "
                    + genericSettings.getSimpleName() + ". Report driver developer.";
            logger.error(errorMessage);
            sb.append(errorMessage);
        }
        else {
            sb.append("Synopsis:");
            boolean first = true;
            try {
                Method valueMethod = options.getMethod(PREFIX);
                Method mandatorylMethod = options.getMethod(MANDATORY);

                for (Enum<? extends OptionI> option : options.getEnumConstants()) {
                    boolean mandatory = (boolean) mandatorylMethod.invoke(option);
                    String value = (String) valueMethod.invoke(option);
                    if (mandatory) {
                        if (!first) {
                            sb.append(SEPARATOR);
                        }
                        first = false;
                        sb.append(' ' + value + PAIR_SEP + " <" + option.name().toLowerCase(LOCALE) + '>');
                    }
                    else {
                        sbNotMandetory.append(
                                " [" + SEPARATOR + value + PAIR_SEP + " <" + option.name().toLowerCase(LOCALE) + ">]");
                    }
                }
                sb.append(sbNotMandetory);
            } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException
                    | NoSuchMethodException | SecurityException e) {
                String errorMessage = "Driver implementation error, in method syntax(). Could not find method. Report driver developer.";
                logger.error(errorMessage);
                sb.append(errorMessage);
            }
        }
        return sb.toString();
    }

    public interface OptionI {

        String prefix();

        Class<?> type();

        boolean mandatory();
    }

    /**
     * Example Option Enum
     */
    @SuppressWarnings("unused")
    private static enum Option implements OptionI {
        EXAMPLE0("ex0", Integer.class, false),
        EXAMPLE1("ex1", String.class, true);

        private final String prefix;
        private final Class<?> type;
        private final boolean mandatory;

        private Option(String prefix, Class<?> type, boolean mandatory) {
            this.prefix = prefix;
            this.type = type;
            this.mandatory = mandatory;
        }

        @Override
        public String prefix() {
            return this.prefix;
        }

        @Override
        public Class<?> type() {
            return this.type;
        }

        @Override
        public boolean mandatory() {
            return this.mandatory;
        }
    }

    synchronized int parseFields(String settings, Class<? extends Enum<? extends OptionI>> options)
            throws ArgumentSyntaxException {
        String enclosingClassName = options.getEnclosingClass().getSimpleName();
        int enumValuesLength = options.getEnumConstants().length;

        Method prefixMethod;
        Method typeMethod;
        Method mandatorylMethod;

        String[] settingsArray = settings.trim().split(SEPARATOR);
        int settingsArrayLength = settingsArray.length;

        if (settingsArrayLength >= 1 && settingsArrayLength <= enumValuesLength) {
            try {
                prefixMethod = options.getMethod(PREFIX);
                typeMethod = options.getMethod(TYPE);
                mandatorylMethod = options.getMethod(MANDATORY);
            } catch (NoSuchMethodException | SecurityException e) {
                throw new ArgumentSyntaxException("Driver implementation error, \'" + enclosingClassName
                        + "\' problem to find method in implementation. Report driver developer.\n" + e);
            }

            try {
                for (Enum<? extends OptionI> option : options.getEnumConstants()) {
                    String prefix = (String) prefixMethod.invoke(option);
                    Class<?> type = (Class<?>) typeMethod.invoke(option);
                    boolean mandatory = (boolean) mandatorylMethod.invoke(option);
                    boolean noOptionsPresent = true;
                    String setting = "";

                    for (String singlesetting : settingsArray) {
                        setting = singlesetting.trim();
                        String[] pair = setting.split(PAIR_SEP);
                        int pairLength = pair.length;

                        if (mandatory && pairLength != 2) {
                            throw new ArgumentSyntaxException("Parameter in " + enclosingClassName
                                    + " is not a pair of prefix and value: <prefix>" + PAIR_SEP + "<value> ");
                        }
                        if (pairLength == 2 && pair[0].trim().equals(prefix)) {
                            try {
                                noOptionsPresent = false;
                                setField(pair[1], option.name(), type, options);
                            } catch (NoSuchFieldException | IllegalAccessException e) {
                                throw new ArgumentSyntaxException("Driver implementation error, \'" + enclosingClassName
                                        + "\' has no corresponding field for parameter " + setting
                                        + ". Report driver developer.\n" + e);
                            }
                        }
                    }
                    if (noOptionsPresent && mandatory) {
                        throw new ArgumentSyntaxException("Mandatory parameter " + option.name() + " is nor present in "
                                + this.getClass().getSimpleName());
                    }
                }
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                throw new ArgumentSyntaxException(
                        "Driver implementation error, \'" + options.getName().toLowerCase(LOCALE)
                                + "\' problem to invoke method. Report driver developer.\n" + e);
            }

        }
        else if (settingsArrayLength > enumValuesLength) {
            throw new ArgumentSyntaxException("Too much parameters in " + enclosingClassName + ".");
        }
        return settingsArrayLength;
    }

    private synchronized void setField(String value, String enumName, Class<?> type,
            Class<? extends Enum<? extends OptionI>> options)
            throws IllegalAccessException, NoSuchFieldException, ArgumentSyntaxException {
        String optionName = enumName.toLowerCase(LOCALE);
        value = value.trim();

        switch (type.getSimpleName()) {
        case "Boolean":
            options.getDeclaringClass().getDeclaredField(optionName).setBoolean(this, extractBoolean(value, enumName));
            break;
        case "Short":
            options.getDeclaringClass().getDeclaredField(optionName).setShort(this, extractShort(value, enumName));
            break;
        case "Integer":
            options.getDeclaringClass().getDeclaredField(optionName).setInt(this, extractInteger(value, enumName));
            break;
        case "Long":
            options.getDeclaringClass().getDeclaredField(optionName).setLong(this, extractLong(value, enumName));
            break;
        case "Float":
            options.getDeclaringClass().getDeclaredField(optionName).setFloat(this, extractFloat(value, enumName));
            break;
        case "Double":
            options.getDeclaringClass().getDeclaredField(optionName).setDouble(this, extractDouble(value, enumName));
            break;
        case "String":
            options.getDeclaringClass().getDeclaredField(optionName).set(this, value);
            break;
        case "InetAddress":
            options.getDeclaringClass().getDeclaredField(optionName).set(this, extractInetAddress(value, enumName));
            break;
        default:
            throw new NoSuchFieldException("Driver implementation error, \'" + enumName.toLowerCase(LOCALE)
                    + "\' not supported data type. Report driver developer\n");
        }
    }

    private synchronized boolean extractBoolean(String value, String errorMessage) throws ArgumentSyntaxException {
        Boolean ret = false;
        try {
            ret = Boolean.getBoolean(value);
        } catch (NumberFormatException e) {
            argumentSyntaxException(errorMessage, ret.getClass().getSimpleName());
        }
        return ret;
    }

    private synchronized short extractShort(String value, String errorMessage) throws ArgumentSyntaxException {
        Short ret = 0;
        try {
            ret = Short.decode(value);
        } catch (NumberFormatException e) {
            argumentSyntaxException(errorMessage, ret.getClass().getSimpleName());
        }
        return ret;
    }

    private synchronized int extractInteger(String value, String errorMessage) throws ArgumentSyntaxException {
        Integer ret = 0;
        try {
            ret = Integer.decode(value);
        } catch (NumberFormatException e) {
            argumentSyntaxException(errorMessage, ret.getClass().getSimpleName());
        }
        return ret;
    }

    private synchronized long extractLong(String value, String errorMessage) throws ArgumentSyntaxException {
        Long ret = 0l;
        try {
            ret = Long.decode(value);
        } catch (NumberFormatException e) {
            argumentSyntaxException(errorMessage, ret.getClass().getSimpleName());
        }
        return ret;
    }

    private synchronized float extractFloat(String value, String errorMessage) throws ArgumentSyntaxException {
        Float ret = 0f;
        try {
            ret = Float.parseFloat(value);
        } catch (NumberFormatException e) {
            argumentSyntaxException(errorMessage, ret.getClass().getSimpleName());
        }
        return ret;
    }

    private synchronized double extractDouble(String value, String errorMessage) throws ArgumentSyntaxException {
        Double ret = 0.;
        try {
            ret = Double.parseDouble(value);
        } catch (NumberFormatException e) {
            argumentSyntaxException(errorMessage, ret.getClass().getSimpleName());
        }
        return ret;
    }

    private synchronized InetAddress extractInetAddress(String value, String errorMessage)
            throws ArgumentSyntaxException {
        InetAddress ret = null;
        try {
            ret = InetAddress.getByName(value);
        } catch (UnknownHostException e) {
            argumentSyntaxException(errorMessage, "InetAddress");
        }
        return ret;
    }

    private synchronized void argumentSyntaxException(String errorMessage, String returnType)
            throws ArgumentSyntaxException {
        throw new ArgumentSyntaxException(MessageFormat.format("Value of {0} in {1} is not type of {2}.", errorMessage,
                this.getClass().getSimpleName(), returnType));
    }

}
