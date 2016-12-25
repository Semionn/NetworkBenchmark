package com.au.mit.benchmark.network.common.exceptions;

public class RepeatedServerStartException extends RuntimeException {
    public RepeatedServerStartException() {
    }

    public RepeatedServerStartException(String message) {
        super(message);
    }
}
