package com.sahaj.smartparkinglot.infrastructure.memory;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class UuidTicketIdGeneratorTest {
    @Test
    void generatesDistinctRandomVersionFourUuids() {
        UuidTicketIdGenerator generator = new UuidTicketIdGenerator();

        UUID first = generator.nextId();
        UUID second = generator.nextId();

        assertNotEquals(first, second);
        assertEquals(4, first.version());
        assertEquals(4, second.version());
        assertEquals(2, first.variant());
        assertEquals(2, second.variant());
    }
}
