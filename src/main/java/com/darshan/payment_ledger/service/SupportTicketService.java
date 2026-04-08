package com.darshan.payment_ledger.service;

import com.darshan.payment_ledger.dto.PagedResponse;
import com.darshan.payment_ledger.dto.SupportTicketRequest;
import com.darshan.payment_ledger.dto.SupportTicketResponse;
import com.darshan.payment_ledger.dto.TicketUpdateRequest;
import com.darshan.payment_ledger.entity.SupportTicket;
import com.darshan.payment_ledger.entity.User;
import com.darshan.payment_ledger.enums.TicketStatus;
import com.darshan.payment_ledger.repository.SupportTicketRepository;
import com.darshan.payment_ledger.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SupportTicketService {

    private final SupportTicketRepository ticketRepo;
    private final UserRepository          userRepo;

    // ── Customer: create ticket ──────────────────────────────────────────────
    public SupportTicketResponse create(SupportTicketRequest req) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        SupportTicket ticket = SupportTicket.builder()
                .userId(user.getId())
                .subject(req.getSubject())
                .description(req.getDescription())
                .referenceId(req.getReferenceId())
                .status(TicketStatus.OPEN)
                .build();

        return toResponse(ticketRepo.save(ticket), user.getUsername());
    }

    // ── Customer: my tickets ─────────────────────────────────────────────────
    public PagedResponse<SupportTicketResponse> myTickets(Pageable pageable) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Page<SupportTicket> page = ticketRepo.findByUserId(user.getId(), pageable);
        return toPagedResponse(page);
    }

    // ── Backoffice: all tickets (optionally filtered by status) ──────────────
    public PagedResponse<SupportTicketResponse> allTickets(String statusFilter, Pageable pageable) {
        Page<SupportTicket> page;
        if (statusFilter != null && !statusFilter.isBlank()) {
            TicketStatus s = TicketStatus.valueOf(statusFilter.toUpperCase());
            page = ticketRepo.findByStatus(s, pageable);
        } else {
            page = ticketRepo.findAll(pageable);
        }
        return toPagedResponse(page);
    }

    // ── Backoffice: update status + resolution ───────────────────────────────
    public SupportTicketResponse update(Long ticketId, TicketUpdateRequest req) {
        SupportTicket ticket = ticketRepo.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));

        String agentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User agent = userRepo.findByUsername(agentUsername).orElse(null);

        if (req.getStatus() != null) {
            ticket.setStatus(TicketStatus.valueOf(req.getStatus().toUpperCase()));
        }
        if (req.getResolution() != null) {
            ticket.setResolution(req.getResolution());
        }
        if (agent != null) {
            ticket.setResolvedBy(agent.getId());
        }

        SupportTicket saved = ticketRepo.save(ticket);

        // Resolve username of the customer who opened the ticket
        String ownerUsername = userRepo.findById(saved.getUserId())
                .map(User::getUsername).orElse("unknown");

        return toResponse(saved, ownerUsername);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────
    private PagedResponse<SupportTicketResponse> toPagedResponse(Page<SupportTicket> page) {
        var content = page.getContent().stream().map(t -> {
            String uname = userRepo.findById(t.getUserId())
                    .map(User::getUsername).orElse("unknown");
            return toResponse(t, uname);
        }).toList();

        return PagedResponse.<SupportTicketResponse>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    private SupportTicketResponse toResponse(SupportTicket t, String username) {
        return SupportTicketResponse.builder()
                .id(t.getId())
                .userId(t.getUserId())
                .username(username)
                .subject(t.getSubject())
                .description(t.getDescription())
                .referenceId(t.getReferenceId())
                .status(t.getStatus().name())
                .resolution(t.getResolution())
                .resolvedBy(t.getResolvedBy())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }
}
