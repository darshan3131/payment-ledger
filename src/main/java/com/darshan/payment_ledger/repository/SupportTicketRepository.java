package com.darshan.payment_ledger.repository;

import com.darshan.payment_ledger.entity.SupportTicket;
import com.darshan.payment_ledger.enums.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {

    // Customer: their own tickets, paginated
    Page<SupportTicket> findByUserId(Long userId, Pageable pageable);

    // Backoffice: filter by status
    Page<SupportTicket> findByStatus(TicketStatus status, Pageable pageable);
}
