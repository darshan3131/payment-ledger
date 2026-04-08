package com.darshan.payment_ledger.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SupportTicketResponse {
    private Long id;
    private Long userId;
    private String username;      // resolved from User for display
    private String subject;
    private String description;
    private String referenceId;
    private String status;
    private String resolution;
    private Long resolvedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
