package com.darshan.payment_ledger.dto;

import lombok.Data;

@Data
public class TicketUpdateRequest {
    // OPEN | IN_PROGRESS | RESOLVED | CLOSED
    private String status;
    private String resolution;
}
