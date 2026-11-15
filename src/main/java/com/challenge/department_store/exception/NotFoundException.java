package com.challenge.department_store.exception;

public class NotFoundException extends RuntimeException {
    
    public NotFoundException(String msg) {
        super(msg);
    }
}
