package com.ubs.tariffapp.exceptions;

/**
 * Exception thrown when data cleaning operations encounter invalid data.
 */
public class DataCleaningException extends RuntimeException {
    
    private final int lineNumber;
    
    public DataCleaningException(String message, int lineNumber) {
        super(message);
        this.lineNumber = lineNumber;
    }
    
    public DataCleaningException(String message, int lineNumber, Throwable cause) {
        super(message, cause);
        this.lineNumber = lineNumber;
    }
    
    public int getLineNumber() {
        return lineNumber;
    }
}
