package com.darshan.payment_ledger.enums;

// Three roles, one per portal:
// CUSTOMER   → can view own ledger, send money (port 3000)
// BACKOFFICE → can create accounts, view all transactions, audit ledger (port 3001)
// ADMIN      → full access, analytics, health dashboard (port 3002)
//
// Spring Security expects roles to be prefixed with "ROLE_" internally.
// When we store "CUSTOMER" in DB and call hasRole("CUSTOMER") in SecurityConfig,
// Spring automatically checks for "ROLE_CUSTOMER". The prefix is added by Spring — not us.
public enum Role {
    CUSTOMER,
    BACKOFFICE,
    ADMIN
}
