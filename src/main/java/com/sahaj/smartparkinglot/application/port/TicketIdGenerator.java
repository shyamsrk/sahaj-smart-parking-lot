package com.sahaj.smartparkinglot.application.port;

import java.util.UUID;

@FunctionalInterface
public interface TicketIdGenerator {
    UUID nextId();
}
