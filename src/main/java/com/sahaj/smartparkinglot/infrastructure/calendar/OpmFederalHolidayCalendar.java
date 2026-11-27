package com.sahaj.smartparkinglot.infrastructure.calendar;

import com.sahaj.smartparkinglot.domain.exception.InvalidPricingConfigurationException;
import com.sahaj.smartparkinglot.domain.time.FederalHolidayCalendar;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class OpmFederalHolidayCalendar implements FederalHolidayCalendar {
    private static final String RESOURCE_TEMPLATE = "holidays/us-federal-%d.txt";

    private final ClassLoader resourceLoader;
    private final ConcurrentMap<Integer, Set<LocalDate>> holidaysByYear = new ConcurrentHashMap<>();

    public OpmFederalHolidayCalendar() {
        this(OpmFederalHolidayCalendar.class.getClassLoader());
    }

    public OpmFederalHolidayCalendar(ClassLoader resourceLoader) {
        if (resourceLoader == null) {
            throw new IllegalArgumentException("Resource class loader is required");
        }
        this.resourceLoader = resourceLoader;
    }

    @Override
    public boolean isHoliday(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("Date is required");
        }
        return holidaysByYear.computeIfAbsent(date.getYear(), this::loadYear).contains(date);
    }

    private Set<LocalDate> loadYear(int year) {
        String resourceName = RESOURCE_TEMPLATE.formatted(year);
        InputStream input = resourceLoader.getResourceAsStream(resourceName);
        if (input == null) {
            throw new InvalidPricingConfigurationException();
        }

        Set<LocalDate> holidays = new HashSet<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String value = line.strip();
                if (value.isEmpty() || value.startsWith("#")) {
                    continue;
                }

                LocalDate holiday;
                try {
                    holiday = LocalDate.parse(value);
                } catch (DateTimeParseException exception) {
                    throw new InvalidPricingConfigurationException(exception);
                }
                if (holiday.getYear() != year || !holidays.add(holiday)) {
                    throw new InvalidPricingConfigurationException();
                }
            }
        } catch (IOException exception) {
            throw new InvalidPricingConfigurationException(exception);
        }

        if (holidays.isEmpty()) {
            throw new InvalidPricingConfigurationException();
        }
        return Set.copyOf(holidays);
    }
}
