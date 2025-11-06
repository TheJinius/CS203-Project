package com.ubs.tariffapp.exceptions;

/**
 * Exception thrown when duty rate parsing fails.
 */
public class DutyParsingException extends RuntimeException {
    
    public DutyParsingException(String message) {
        super(message);
    }
    
    public DutyParsingException(String message, Throwable cause) {
        super(message, cause);
    }
}
