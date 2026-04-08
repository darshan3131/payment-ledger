package com.darshan.payment_ledger.entity;

import com.darshan.payment_ledger.enums.AccountStatus;
import com.darshan.payment_ledger.enums.Currency;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// @Version enables optimistic locking.
// When two concurrent transactions read the same account and both try to update,
// Hibernate checks: "has the version number changed since I read it?"
// If yes → someone else updated first → throw ObjectOptimisticLockingFailureException.
// This prevents the "lost update" problem without using DB-level row locks (PESSIMISTIC locking).
// Optimistic = assume no conflict, verify at write time. Cheaper for low-contention scenarios.

import java.time.LocalDateTime;

@Entity
@Table(name = "accounts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String accountNumber;

    @Column(nullable = false)
    private String holderName;

    // FK link to the owning User (CUSTOMER role).
    // Nullable because the bootstrap SYSTEM_CASH account has no owning user.
    @Column(name = "user_id")
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Currency currency;

    @Column(nullable = false)
    private Long balance;

    // This is the optimistic locking version counter.
    // Hibernate auto-increments this on every UPDATE.
    // If two threads read version=5 and both try to write,
    // the second write sees version=6 (changed by the first) and throws.
    @Version
    private Long version;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}