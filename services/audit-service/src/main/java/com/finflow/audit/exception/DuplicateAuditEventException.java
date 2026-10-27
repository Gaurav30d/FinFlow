package com.finflow.audit.exception;

/**
 * Exception thrown when an audit event with the same unique eventId already exists.
 */
public class DuplicateAuditEventException extends RuntimeException {

    public DuplicateAuditEventException(String message) {
        super(message);
    }

    public DuplicateAuditEventException(String message, Throwable cause) {
        super(message, cause);
    }
}
