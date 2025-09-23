package com.ubs.tariffapp.exceptions;

public class DutyNotFoundException extends RuntimeException {
    public DutyNotFoundException(String message) {
        super(message);
    }
}