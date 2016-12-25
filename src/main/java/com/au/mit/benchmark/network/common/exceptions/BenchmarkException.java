package com.au.mit.benchmark.network.common.exceptions;

public class BenchmarkException extends RuntimeException {
    public BenchmarkException() {
    }

    public BenchmarkException(String message) {
        super(message);
    }

    public BenchmarkException(String message, Throwable cause) {
        super(message, cause);
    }
}
