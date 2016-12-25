package com.au.mit.benchmark.network.common.exceptions;

/**
 * Created by Semionn on 15.12.2016.
 */
public class CommunicationException extends RuntimeException {
    public CommunicationException() {
    }

    public CommunicationException(String message) {
        super(message);
    }

    public CommunicationException(Throwable cause) {
        super(cause);
    }
}
