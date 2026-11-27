package com.sahaj.smartparkinglot.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MoneyTest {
    @Test
    void performsExactDecimalArithmeticAndComparesByValue() {
        Money amount = Money.of("5.00").add(Money.of("3.00")).multiply(new BigDecimal("0.8"));

        assertEquals(Money.of("6.400"), amount);
        assertEquals(0, Money.of("6.40").compareTo(amount));
    }

    @Test
    void roundsHalfUpToCentsOnlyWhenRequested() {
        assertEquals(Money.of("10.01"), Money.of("10.005").roundToCents());
        assertEquals(Money.of("10.00"), Money.of("10.004").roundToCents());
        assertEquals(Money.of("10.005"), Money.of("10.005"));
    }

    @Test
    void rejectsNegativeAmountsAndMultipliers() {
        assertThrows(IllegalArgumentException.class, () -> Money.of("-0.01"));
        assertThrows(IllegalArgumentException.class,
                () -> Money.of("1").multiply(new BigDecimal("-1")));
    }

    @Test
    void preservesValueEqualityHashingAndPlainStringRepresentation() {
        Money amount = Money.of("10.500");

        assertEquals(new BigDecimal("10.500"), amount.amount());
        assertEquals(amount, amount);
        assertEquals(amount, Money.of("10.50"));
        assertEquals(amount.hashCode(), Money.of("10.50").hashCode());
        assertEquals("10.500", amount.toString());
        assertNotEquals(amount, Money.of("10.51"));
        assertNotEquals(amount, "10.500");
    }

    @Test
    void rejectsNullAmountsAndOperands() {
        assertThrows(NullPointerException.class, () -> Money.of((BigDecimal) null));
        assertThrows(NullPointerException.class, () -> Money.of("1.00").add(null));
        assertThrows(NullPointerException.class, () -> Money.of("1.00").multiply(null));
        assertThrows(NullPointerException.class, () -> Money.of("1.00").compareTo(null));
    }
}
