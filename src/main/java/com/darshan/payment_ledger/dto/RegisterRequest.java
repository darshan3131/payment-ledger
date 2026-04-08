package com.darshan.payment_ledger.dto;

import com.darshan.payment_ledger.enums.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

// What the client sends to POST /api/v1/auth/register
// In production you'd restrict who can create ADMIN/BACKOFFICE users.
// For now, registration is open (useful for development/testing).
@Data
public class RegisterRequest {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;

    @NotNull(message = "Role is required")
    private Role role;

    // Phone in E.164 format — REQUIRED for self-registration (OTP verification)
    @Pattern(regexp = "^\\+[1-9]\\d{7,14}$", message = "Phone must be in E.164 format: +91XXXXXXXXXX")
    private String phone;

    // OTP received via SMS — REQUIRED for self-registration, ignored for admin-created users
    private String otp;
}
