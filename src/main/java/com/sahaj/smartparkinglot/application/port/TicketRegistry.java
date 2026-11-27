package com.sahaj.smartparkinglot.application.port;

import com.sahaj.smartparkinglot.domain.ParkingTicket;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TicketRegistry {
    Optional<ParkingTicket> findById(UUID ticketId);

    List<ParkingTicket> findAll();

    void register(ParkingTicket ticket);
}
