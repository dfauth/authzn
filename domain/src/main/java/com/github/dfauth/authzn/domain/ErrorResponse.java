package com.github.dfauth.authzn.domain;

public class ErrorResponse {

    private String message;
    private String stacktrace;

    public ErrorResponse(String message, String stacktrace) {
        this.message = message;
        this.stacktrace = stacktrace;
    }

    public String getStacktrace() {
        return stacktrace;
    }

    public String getMessage() {
        return message;
    }
}
