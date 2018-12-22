package org.openmuc.framework.driver.csv.exceptions;

/**
 * General exception of the CsvDriver
 */
public class CsvException extends Exception {

    private static final long serialVersionUID = 5298208874918144896L;

    protected final String message;

    public CsvException(String message) {
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }

}
