package com.au.mit.benchmark.network.common.exceptions;

public class WrongResponseException extends RuntimeException {
    public WrongResponseException() {
    }

    public WrongResponseException(String message) {
        super(message);
    }
}
