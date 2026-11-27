package com.sahaj.smartparkinglot.application;

import com.sahaj.smartparkinglot.domain.FareQuote;

import java.time.Instant;
import java.util.UUID;

public interface ExitService {
    FareQuote exit(UUID ticketId, Instant exitAt);
}
