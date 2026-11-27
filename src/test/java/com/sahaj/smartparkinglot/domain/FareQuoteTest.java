package com.sahaj.smartparkinglot.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FareQuoteTest {
    @Test
    void createsRoundedFinalQuoteFromExactCandidate() {
        FareCandidate candidate = new FareCandidate(PricingPolicyType.EARLY_BIRD, Money.of("10.005"));

        FareQuote quote = FareQuote.from(candidate);

        assertEquals(Money.of("10.005"), quote.exactAmount());
        assertEquals(Money.of("10.01"), quote.amount());
        assertEquals(PricingPolicyType.EARLY_BIRD, quote.policy());
    }

    @Test
    void rejectsFinalAmountThatDoesNotMatchHalfUpRounding() {
        assertThrows(IllegalArgumentException.class,
                () -> new FareQuote(PricingPolicyType.STANDARD_HOURLY,
                        Money.of("10.005"), Money.of("10.00")));
    }

    @Test
    void exposesTieBreakPriority() {
        assertEquals(0, PricingPolicyType.EARLY_BIRD.tieBreakPriority());
        assertEquals(1, PricingPolicyType.NIGHT_OWL.tieBreakPriority());
        assertEquals(2, PricingPolicyType.STANDARD_HOURLY.tieBreakPriority());
    }
}
