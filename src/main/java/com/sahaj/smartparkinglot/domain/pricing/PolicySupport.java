package com.sahaj.smartparkinglot.domain.pricing;

import com.sahaj.smartparkinglot.domain.LoyaltyTier;
import com.sahaj.smartparkinglot.domain.Money;
import com.sahaj.smartparkinglot.domain.ParkingStay;
import com.sahaj.smartparkinglot.domain.time.PricingContext;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;

final class PolicySupport {
    private static final Duration MAX_SPECIAL_STAY = Duration.ofHours(24);
    private static final BigDecimal ONE = BigDecimal.ONE;

    private PolicySupport() {
    }

    static boolean isSpecialStayTooLong(ParkingStay stay) {
        return stay.elapsedDuration().compareTo(MAX_SPECIAL_STAY) > 0;
    }

    static boolean isEligibleWeekday(LocalDate date, PricingContext context) {
        DayOfWeek day = date.getDayOfWeek();
        return day != DayOfWeek.SATURDAY
                && day != DayOfWeek.SUNDAY
                && !context.holidayCalendar().isHoliday(date);
    }

    static boolean isWithinHalfOpenWindow(LocalTime time, LocalTime start, LocalTime end) {
        return !time.isBefore(start) && time.isBefore(end);
    }

    static Money specialFare(String carRate, LoyaltyTier loyaltyTier, BigDecimal vehicleMultiplier) {
        BigDecimal discountedRate = new BigDecimal(carRate)
                .multiply(ONE.subtract(loyaltyTier.discountRate()));
        return Money.of(discountedRate).multiply(vehicleMultiplier);
    }
}
