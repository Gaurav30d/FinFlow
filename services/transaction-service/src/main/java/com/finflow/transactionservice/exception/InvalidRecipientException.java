package com.finflow.transactionservice.exception;

public class InvalidRecipientException extends RuntimeException {

    public InvalidRecipientException(String message) {
        super(message);
    }
}
