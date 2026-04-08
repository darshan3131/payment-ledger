package com.darshan.payment_ledger.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

// What the client sends to POST /api/v1/auth/login
@Data
public class LoginRequest {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;
}
