package com.sribalaji.erp.exception;

/** Thrown for any business-rule violation (insufficient stock, invalid state, etc.) that should surface as a user-facing error. */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
