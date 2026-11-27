package com.sahaj.smartparkinglot.infrastructure.calendar;

import com.sahaj.smartparkinglot.domain.exception.InvalidPricingConfigurationException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpmFederalHolidayCalendarTest {
    @Test
    void loadsOfficialAndObservedDatesFromAnnualResources() {
        OpmFederalHolidayCalendar calendar = new OpmFederalHolidayCalendar();

        assertTrue(calendar.isHoliday(LocalDate.of(2026, 7, 3)));
        assertTrue(calendar.isHoliday(LocalDate.of(2026, 7, 4)));
        assertFalse(calendar.isHoliday(LocalDate.of(2026, 7, 6)));
        assertTrue(calendar.isHoliday(LocalDate.of(2027, 12, 31)));
        assertTrue(calendar.isHoliday(LocalDate.of(2028, 1, 1)));
    }

    @Test
    void failsExplicitlyWhenYearResourceIsMissing() {
        OpmFederalHolidayCalendar calendar = new OpmFederalHolidayCalendar(emptyClassLoader());

        assertThrows(InvalidPricingConfigurationException.class,
                () -> calendar.isHoliday(LocalDate.of(2030, 1, 1)));
    }

    @Test
    void rejectsMissingClassLoaderAndDate() {
        assertThrows(IllegalArgumentException.class,
                () -> new OpmFederalHolidayCalendar(null));

        OpmFederalHolidayCalendar calendar = new OpmFederalHolidayCalendar(emptyClassLoader());
        assertThrows(IllegalArgumentException.class, () -> calendar.isHoliday(null));
    }

    @Test
    void rejectsMalformedDuplicateWrongYearAndEmptyResources() {
        assertInvalidResource("not-a-date\n");
        assertInvalidResource("2030-01-01\n2030-01-01\n");
        assertInvalidResource("2029-12-31\n");
        assertInvalidResource("\n# no dates\n");
        assertInvalidResource("# no dates\n");
    }

    @Test
    void wrapsResourceReadFailuresAsInvalidConfiguration() {
        ClassLoader loader = new ClassLoader(null) {
            @Override
            public InputStream getResourceAsStream(String name) {
                if (!"holidays/us-federal-2030.txt".equals(name)) {
                    return null;
                }
                return new InputStream() {
                    @Override
                    public int read() throws IOException {
                        throw new IOException("Unable to read holiday resource");
                    }
                };
            }
        };
        OpmFederalHolidayCalendar calendar = new OpmFederalHolidayCalendar(loader);

        InvalidPricingConfigurationException exception = assertThrows(
                InvalidPricingConfigurationException.class,
                () -> calendar.isHoliday(LocalDate.of(2030, 1, 1)));
        assertTrue(exception.getCause() instanceof IOException);
    }

    private static void assertInvalidResource(String contents) {
        ClassLoader loader = resourceClassLoader(
                "holidays/us-federal-2030.txt", contents);
        OpmFederalHolidayCalendar calendar = new OpmFederalHolidayCalendar(loader);
        assertThrows(InvalidPricingConfigurationException.class,
                () -> calendar.isHoliday(LocalDate.of(2030, 1, 1)));
    }

    private static ClassLoader emptyClassLoader() {
        return resourceClassLoader("no-resource.txt", "");
    }

    private static ClassLoader resourceClassLoader(String resourceName, String contents) {
        return new ClassLoader(null) {
            @Override
            public java.io.InputStream getResourceAsStream(String name) {
                if (!resourceName.equals(name)) {
                    return null;
                }
                return new ByteArrayInputStream(contents.getBytes(StandardCharsets.UTF_8));
            }
        };
    }
}
