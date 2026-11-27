package com.sahaj.smartparkinglot.domain.pricing;

import com.sahaj.smartparkinglot.domain.FareCandidate;
import com.sahaj.smartparkinglot.domain.Money;
import com.sahaj.smartparkinglot.domain.ParkingStay;
import com.sahaj.smartparkinglot.domain.PricingPolicyType;
import com.sahaj.smartparkinglot.domain.time.PricingContext;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

public final class StandardHourlyPolicy implements RatePolicy {
    private static final long SEGMENT_SECONDS = Duration.ofHours(1).toSeconds();
    private static final BigDecimal PEAK_MULTIPLIER = new BigDecimal("1.5");
    private static final BigDecimal FIRST_HOUR_RATE = new BigDecimal("5.00");
    private static final BigDecimal SECOND_HOUR_RATE = new BigDecimal("3.00");
    private static final BigDecimal LATER_HOUR_RATE = new BigDecimal("2.00");
    private static final List<PeakWindow> PEAK_WINDOWS = List.of(
            new PeakWindow(LocalTime.of(7, 0), LocalTime.of(10, 0)),
            new PeakWindow(LocalTime.of(16, 0), LocalTime.of(19, 0)));

    @Override
    public Optional<FareCandidate> evaluate(ParkingStay stay, PricingContext context) {
        long fullHours = stay.elapsedDuration().toHours();
        long segmentCount = fullHours;
        if (!stay.elapsedDuration().minusHours(fullHours).isZero()) {
            segmentCount++;
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        Instant segmentStart = stay.entryAt();
        for (long segmentIndex = 0; segmentIndex < segmentCount; segmentIndex++) {
            Instant segmentEnd = segmentStart.plusSeconds(SEGMENT_SECONDS);
            BigDecimal rate = rateForSegment(segmentIndex);
            if (overlapsPeakWindow(segmentStart, segmentEnd, context)) {
                rate = rate.multiply(PEAK_MULTIPLIER);
            }
            subtotal = subtotal.add(rate);
            segmentStart = segmentEnd;
        }

        Money fare = Money.of(subtotal).multiply(stay.vehicleType().rateMultiplier());
        return Optional.of(new FareCandidate(PricingPolicyType.STANDARD_HOURLY, fare));
    }

    private static BigDecimal rateForSegment(long segmentIndex) {
        if (segmentIndex == 0) {
            return FIRST_HOUR_RATE;
        }
        return segmentIndex == 1 ? SECOND_HOUR_RATE : LATER_HOUR_RATE;
    }

    private static boolean overlapsPeakWindow(
            Instant segmentStart, Instant segmentEnd, PricingContext context) {
        ZoneId zone = context.zoneId();
        LocalDate date = segmentStart.atZone(zone).toLocalDate();
        LocalDate lastDate = segmentEnd.minusNanos(1).atZone(zone).toLocalDate();
        while (!date.isAfter(lastDate)) {
            if (PolicySupport.isEligibleWeekday(date, context)) {
                for (PeakWindow window : PEAK_WINDOWS) {
                    Instant windowStart = date.atTime(window.start()).atZone(zone).toInstant();
                    Instant windowEnd = date.atTime(window.end()).atZone(zone).toInstant();
                    if (segmentStart.isBefore(windowEnd) && windowStart.isBefore(segmentEnd)) {
                        return true;
                    }
                }
            }
            date = date.plusDays(1);
        }
        return false;
    }

    private record PeakWindow(LocalTime start, LocalTime end) {
    }
}
