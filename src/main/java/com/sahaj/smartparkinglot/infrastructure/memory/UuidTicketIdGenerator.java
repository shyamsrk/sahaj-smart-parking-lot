package com.sahaj.smartparkinglot.infrastructure.memory;

import com.sahaj.smartparkinglot.application.port.TicketIdGenerator;

import java.util.UUID;

public final class UuidTicketIdGenerator implements TicketIdGenerator {
    @Override
    public UUID nextId() {
        return UUID.randomUUID();
    }
}
