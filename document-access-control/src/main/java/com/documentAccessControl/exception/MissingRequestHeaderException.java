package com.documentAccessControl.exception;

public class MissingRequestHeaderException extends RuntimeException {
    public MissingRequestHeaderException(String message, String s) {
        super(message);
    }
}
