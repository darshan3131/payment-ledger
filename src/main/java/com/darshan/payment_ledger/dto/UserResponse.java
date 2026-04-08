package com.darshan.payment_ledger.dto;

import com.darshan.payment_ledger.entity.User;
import com.darshan.payment_ledger.enums.Role;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

// Safe outbound representation of a User — never exposes the password hash.
@Data
@Builder
public class UserResponse {

    private Long          id;
    private String        username;
    private Role          role;
    private String        accountNumber;
    private String        phone;
    private boolean       enabled;
    private LocalDateTime createdAt;

    public static UserResponse from(User u) {
        return UserResponse.builder()
                .id(u.getId())
                .username(u.getUsername())
                .role(u.getRole())
                .accountNumber(u.getAccountNumber())
                .phone(u.getPhone())
                .enabled(u.isEnabled())
                .createdAt(u.getCreatedAt())
                .build();
    }
}
