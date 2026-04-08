package com.darshan.payment_ledger.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ForgotPasswordRequest {

    // E.164 format: +91XXXXXXXXXX (India), +1XXXXXXXXXX (US), etc.
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+[1-9]\\d{7,14}$", message = "Phone must be in E.164 format: +91XXXXXXXXXX")
    private String phone;
}
