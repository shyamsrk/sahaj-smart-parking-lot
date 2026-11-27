package com.sahaj.smartparkinglot.domain.pricing;

import com.sahaj.smartparkinglot.domain.FareCandidate;
import com.sahaj.smartparkinglot.domain.ParkingStay;
import com.sahaj.smartparkinglot.domain.PricingPolicyType;
import com.sahaj.smartparkinglot.domain.time.PricingContext;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

public final class NightOwlPolicy implements RatePolicy {
    private static final LocalTime ENTRY_START = LocalTime.of(18, 0);
    private static final LocalTime EXIT_START = LocalTime.of(5, 0);
    private static final LocalTime EXIT_END = LocalTime.of(10, 0);
    private static final String CAR_RATE = "8.00";

    @Override
    public Optional<FareCandidate> evaluate(ParkingStay stay, PricingContext context) {
        if (PolicySupport.isSpecialStayTooLong(stay)) {
            return Optional.empty();
        }

        LocalDate entryDate = stay.entryAt().atZone(context.zoneId()).toLocalDate();
        LocalDate exitDate = stay.exitAt().atZone(context.zoneId()).toLocalDate();
        if (!entryDate.plusDays(1).equals(exitDate)
                || !PolicySupport.isEligibleWeekday(entryDate, context)
                || !PolicySupport.isEligibleWeekday(exitDate, context)) {
            return Optional.empty();
        }

        LocalTime entryTime = stay.entryAt().atZone(context.zoneId()).toLocalTime();
        LocalTime exitTime = stay.exitAt().atZone(context.zoneId()).toLocalTime();
        if (entryTime.isBefore(ENTRY_START)
                || !PolicySupport.isWithinHalfOpenWindow(exitTime, EXIT_START, EXIT_END)) {
            return Optional.empty();
        }

        return Optional.of(new FareCandidate(
                PricingPolicyType.NIGHT_OWL,
                PolicySupport.specialFare(
                        CAR_RATE, stay.loyaltyTier(), stay.vehicleType().rateMultiplier())));
    }
}
