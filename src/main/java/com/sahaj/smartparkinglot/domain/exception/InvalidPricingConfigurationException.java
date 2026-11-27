package com.sahaj.smartparkinglot.domain.exception;

public final class InvalidPricingConfigurationException extends IllegalStateException {
    public InvalidPricingConfigurationException() {
    }

    public InvalidPricingConfigurationException(Throwable cause) {
        super(cause);
    }
}
