package com.finflow.transactionservice.ai.client;

import lombok.Getter;

@Getter
public class GeminiException extends RuntimeException {

    private final int statusCode;
    private final String errorCategory;

    public GeminiException(String message) {
        super(message);
        this.statusCode = 500;
        this.errorCategory = "ERROR";
    }

    public GeminiException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = 500;
        this.errorCategory = "ERROR";
    }

    public GeminiException(String message, int statusCode, String errorCategory) {
        super(message);
        this.statusCode = statusCode;
        this.errorCategory = errorCategory;
    }

    public GeminiException(String message, Throwable cause, int statusCode, String errorCategory) {
        super(message, cause);
        this.statusCode = statusCode;
        this.errorCategory = errorCategory;
    }
}
