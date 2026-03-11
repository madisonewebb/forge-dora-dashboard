package com.liatrio.dora.exception;

public class InsightsUnavailableException extends RuntimeException {

    public InsightsUnavailableException(String reason) {
        super(reason);
    }
}
