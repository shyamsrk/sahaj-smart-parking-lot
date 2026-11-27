package com.sahaj.smartparkinglot.application.port;

import com.sahaj.smartparkinglot.domain.ParkingTicket;

import java.util.Optional;
import java.util.UUID;

public interface TicketRegistry {
    Optional<ParkingTicket> findById(UUID ticketId);

    void register(ParkingTicket ticket);
}
