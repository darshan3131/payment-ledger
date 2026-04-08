package com.darshan.payment_ledger.entity;

import com.darshan.payment_ledger.enums.Role;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// This is the users table — stores login credentials.
// Separate from Account (the financial entity) by design:
//   Account = where money lives (ACCxxx number, balance, currency)
//   User    = who can log in (username, password hash, role)
// A User can be linked to an Account (customer portal)
// or have no Account (backoffice staff, admin)

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    // NEVER store plain-text passwords.
    // We store the BCrypt hash. BCrypt is a one-way function:
    // "password123" → "$2a$10$..." (60-char hash)
    // On login: BCrypt.matches("password123", storedHash) → true/false
    // Even if DB is stolen, attacker cannot reverse the hash to get passwords.
    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    // Optional: link this user to a specific account (for CUSTOMER role)
    // BACKOFFICE and ADMIN users won't have an accountNumber
    private String accountNumber;

    // Phone number in E.164 format: +91XXXXXXXXXX (India) or +1XXXXXXXXXX (US)
    // Used for OTP-based password reset. Unique when present.
    @Column(unique = true)
    private String phone;

    @Builder.Default
    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
