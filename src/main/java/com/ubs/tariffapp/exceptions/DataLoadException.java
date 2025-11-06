package com.ubs.tariffapp.exceptions;

/**
 * Exception thrown when data loading operations fail.
 */
public class DataLoadException extends RuntimeException {
    
    public DataLoadException(String message) {
        super(message);
    }
    
    public DataLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
