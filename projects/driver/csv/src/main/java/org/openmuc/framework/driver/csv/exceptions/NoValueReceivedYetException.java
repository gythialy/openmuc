package org.openmuc.framework.driver.csv.exceptions;

import org.openmuc.framework.driver.csv.CsvException;

public class NoValueReceivedYetException extends CsvException {

    private static final long serialVersionUID = 8609753792624311525L;

    public NoValueReceivedYetException(String message) {
        super(message);
    }

    @Override
    public String getMessage() {
        return message;
    }
}
