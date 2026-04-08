package com.darshan.payment_ledger.dto;

import com.darshan.payment_ledger.enums.Role;
import lombok.Builder;
import lombok.Data;

// What we send back after successful login
@Data
@Builder
public class LoginResponse {

    private String token;          // the JWT — client stores this and sends on every request
    private String username;
    private Role role;
    private String accountNumber;  // null for BACKOFFICE/ADMIN users
    private long expiresIn;        // milliseconds until token expires (86400000 = 24 hours)
}
