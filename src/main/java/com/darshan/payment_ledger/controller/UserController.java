package com.darshan.payment_ledger.controller;

import com.darshan.payment_ledger.dto.RegisterRequest;
import com.darshan.payment_ledger.dto.UserResponse;
import com.darshan.payment_ledger.entity.User;
import com.darshan.payment_ledger.enums.Role;
import com.darshan.payment_ledger.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// ADMIN-only user management.
// All endpoints here require ROLE_ADMIN — enforced via @PreAuthorize.
// This is how admins create backoffice staff and other admin accounts.

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class UserController {

    private final UserRepository  userRepository;
    private final PasswordEncoder passwordEncoder;

    // GET /api/v1/users
    // Returns all users — summary view (no password hashes).
    // Optional ?role=BACKOFFICE filter.
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> listUsers(
            @RequestParam(required = false) Role role) {

        List<User> users = (role != null)
                ? userRepository.findByRole(role)
                : userRepository.findAll();

        return ResponseEntity.ok(
                users.stream().map(UserResponse::from).toList()
        );
    }

    // GET /api/v1/users/{id}
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getUser(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(u -> ResponseEntity.ok(UserResponse.from(u)))
                .orElse(ResponseEntity.notFound().build());
    }

    // POST /api/v1/users
    // Admin creates a staff account (BACKOFFICE or ADMIN role).
    // Also allows creating CUSTOMER accounts from admin side.
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createUser(@Valid @RequestBody RegisterRequest request,
                                        Authentication authentication) {

        if (userRepository.existsByUsername(request.getUsername())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Username already exists: " + request.getUsername()));
        }

        if (request.getPhone() != null && !request.getPhone().isBlank()
                && userRepository.existsByPhone(request.getPhone())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Phone number already registered"));
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .phone(request.getPhone())
                .enabled(true)
                .build();

        User saved = userRepository.save(user);
        log.info("Admin '{}' created user '{}' with role '{}'",
                authentication.getName(), saved.getUsername(), saved.getRole());

        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(saved));
    }

    // PATCH /api/v1/users/{id}
    // Update mutable fields: accountNumber, phone, role.
    // Username and password are not changed here (password has its own endpoint).
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateUser(@PathVariable Long id,
                                        @RequestBody Map<String, String> updates,
                                        Authentication authentication) {

        User user = userRepository.findById(id).orElse(null);
        if (user == null) return ResponseEntity.notFound().build();

        if (updates.containsKey("accountNumber")) {
            user.setAccountNumber(updates.get("accountNumber").isBlank() ? null : updates.get("accountNumber").trim());
        }
        if (updates.containsKey("phone")) {
            String phone = updates.get("phone");
            if (phone != null && !phone.isBlank()) {
                // Check uniqueness — skip if same user already owns this phone
                if (!phone.equals(user.getPhone()) && userRepository.existsByPhone(phone)) {
                    return ResponseEntity.status(HttpStatus.CONFLICT)
                            .body(Map.of("error", "Phone number already in use."));
                }
                user.setPhone(phone.trim());
            } else {
                user.setPhone(null);
            }
        }
        if (updates.containsKey("role")) {
            try {
                user.setRole(com.darshan.payment_ledger.enums.Role.valueOf(updates.get("role")));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid role."));
            }
        }

        userRepository.save(user);
        log.info("Admin '{}' updated user '{}'", authentication.getName(), user.getUsername());
        return ResponseEntity.ok(UserResponse.from(user));
    }

    // PATCH /api/v1/users/{id}/status?enabled=true|false
    // Enable or disable a user account (soft disable — does not delete).
    // Disabled users cannot log in (Spring Security checks user.isEnabled()).
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> setUserStatus(@PathVariable Long id,
                                           @RequestParam boolean enabled,
                                           Authentication authentication) {

        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        // Guard: admin cannot disable their own account
        if (user.getUsername().equals(authentication.getName()) && !enabled) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "You cannot disable your own account."));
        }

        user.setEnabled(enabled);
        userRepository.save(user);
        log.info("Admin '{}' {} user '{}'",
                authentication.getName(), enabled ? "enabled" : "disabled", user.getUsername());

        return ResponseEntity.ok(UserResponse.from(user));
    }

    // DELETE /api/v1/users/{id}
    // Hard delete — use with caution. Soft disable is preferred in production.
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteUser(@PathVariable Long id,
                                        Authentication authentication) {

        User user = userRepository.findById(id).orElse(null);
        if (user == null) return ResponseEntity.notFound().build();

        if (user.getUsername().equals(authentication.getName())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "You cannot delete your own account."));
        }

        userRepository.delete(user);
        log.info("Admin '{}' deleted user '{}'", authentication.getName(), user.getUsername());
        return ResponseEntity.ok(Map.of("message", "User deleted."));
    }
}
