package com.sahaj.smartparkinglot.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public final class Money implements Comparable<Money> {
    private final BigDecimal amount;

    private Money(BigDecimal amount) {
        Objects.requireNonNull(amount, "Money amount is required");
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("Money amount must not be negative");
        }
        this.amount = amount;
    }

    public static Money of(String amount) {
        return new Money(new BigDecimal(amount));
    }

    public static Money of(BigDecimal amount) {
        return new Money(amount);
    }

    public BigDecimal amount() {
        return amount;
    }

    public Money add(Money other) {
        Objects.requireNonNull(other, "Money value is required");
        return new Money(amount.add(other.amount));
    }

    public Money multiply(BigDecimal factor) {
        Objects.requireNonNull(factor, "Multiplier is required");
        if (factor.signum() < 0) {
            throw new IllegalArgumentException("Multiplier must not be negative");
        }
        return new Money(amount.multiply(factor));
    }

    public Money roundToCents() {
        return new Money(amount.setScale(2, RoundingMode.HALF_UP));
    }

    @Override
    public int compareTo(Money other) {
        Objects.requireNonNull(other, "Money value is required");
        return amount.compareTo(other.amount);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Money money)) {
            return false;
        }
        return amount.compareTo(money.amount) == 0;
    }

    @Override
    public int hashCode() {
        return amount.stripTrailingZeros().hashCode();
    }

    @Override
    public String toString() {
        return amount.toPlainString();
    }
}
