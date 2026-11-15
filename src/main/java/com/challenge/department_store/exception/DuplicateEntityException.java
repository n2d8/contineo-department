package com.challenge.department_store.exception;

public class DuplicateEntityException extends RuntimeException {

    public DuplicateEntityException(String msg) {
        super(msg);
    }
}
