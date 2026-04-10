package com.darshan.payment_ledger.controller;

import com.darshan.payment_ledger.dto.PagedResponse;
import com.darshan.payment_ledger.dto.SupportTicketRequest;
import com.darshan.payment_ledger.dto.SupportTicketResponse;
import com.darshan.payment_ledger.dto.TicketUpdateRequest;
import com.darshan.payment_ledger.service.SupportTicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/support")
@RequiredArgsConstructor
public class SupportTicketController {

    private final SupportTicketService service;

    // POST /api/v1/support  — customer creates a ticket
    @PostMapping
    public ResponseEntity<SupportTicketResponse> create(@Valid @RequestBody SupportTicketRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }

    // GET /api/v1/support/my  — customer sees own tickets
    @GetMapping("/my")
    public ResponseEntity<PagedResponse<SupportTicketResponse>> myTickets(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(service.myTickets(pageable));
    }

    // GET /api/v1/support  — backoffice/admin sees all tickets (optional ?status=OPEN)
    @GetMapping
    public ResponseEntity<PagedResponse<SupportTicketResponse>> all(
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(service.allTickets(status, pageable));
    }

    // PATCH /api/v1/support/{id}  — backoffice/admin updates ticket
    @PatchMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody TicketUpdateRequest req) {
        try {
            return ResponseEntity.ok(service.update(id, req));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
