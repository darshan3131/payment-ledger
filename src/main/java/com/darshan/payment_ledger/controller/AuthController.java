package com.darshan.payment_ledger.controller;

import com.darshan.payment_ledger.dto.*;
import com.darshan.payment_ledger.entity.User;
import com.darshan.payment_ledger.repository.UserRepository;
import com.darshan.payment_ledger.security.JwtUtil;
import com.darshan.payment_ledger.service.OtpService;
import com.darshan.payment_ledger.service.SmsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final OtpService otpService;
    private final SmsService smsService;

    // POST /api/v1/auth/login
    // Takes username + password → returns JWT token
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            // UsernamePasswordAuthenticationToken (2-arg) = "not yet authenticated"
            // AuthenticationManager.authenticate() calls UserDetailsService + PasswordEncoder
            // If password matches → returns Authentication with authorities
            // If wrong password → throws BadCredentialsException
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );

            // Authentication succeeded — generate JWT
            String username = authentication.getName();
            String role = authentication.getAuthorities()
                    .iterator().next()
                    .getAuthority()
                    .replace("ROLE_", "");  // strip prefix: "ROLE_CUSTOMER" → "CUSTOMER"

            String token = jwtUtil.generateToken(username, role);

            // Get account number if this is a customer
            User user = userRepository.findByUsername(username).orElseThrow();

            log.info("Login successful for user '{}' with role '{}'", username, role);

            return ResponseEntity.ok(LoginResponse.builder()
                    .token(token)
                    .username(username)
                    .role(user.getRole())
                    .accountNumber(user.getAccountNumber())
                    .expiresIn(86400000L)  // 24 hours in ms
                    .build());

        } catch (AuthenticationException e) {
            // Wrong username or password
            log.warn("Login failed for username: {}", request.getUsername());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid username or password"));
        }
    }

    // POST /api/v1/auth/register
    // Public self-registration — CUSTOMER role ONLY, phone OTP required.
    // BACKOFFICE and ADMIN accounts must be created by an Admin via POST /api/v1/users.
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {

        // Security gate: public endpoint only allows CUSTOMER self-registration.
        if (request.getRole() != com.darshan.payment_ledger.enums.Role.CUSTOMER) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Only CUSTOMER accounts can be self-registered. Contact your admin."));
        }

        // Phone is MANDATORY for self-registration (OTP flow depends on it).
        if (request.getPhone() == null || request.getPhone().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Phone number is required for registration."));
        }

        // OTP is MANDATORY — must have sent OTP first via /register/send-otp
        if (request.getOtp() == null || request.getOtp().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "OTP is required. Call /register/send-otp first."));
        }

        // Verify OTP — uses the "reg:" prefix to namespace registration OTPs
        if (!otpService.verifyAndConsume("reg:" + request.getPhone(), request.getOtp())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Invalid or expired OTP. Please request a new one."));
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Username already exists: " + request.getUsername()));
        }

        if (userRepository.existsByPhone(request.getPhone())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Phone number already registered."));
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))  // BCrypt hash
                .role(request.getRole())
                .phone(request.getPhone())
                .enabled(true)
                .build();

        userRepository.save(user);
        log.info("Registered new verified user '{}' with role '{}'", request.getUsername(), request.getRole());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of(
                        "message", "User registered successfully",
                        "username", request.getUsername(),
                        "role", request.getRole()
                ));
    }

    // GET /api/v1/auth/me
    // Returns info about the currently logged-in user (requires valid JWT)
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Not authenticated"));
        }

        User user = userRepository.findByUsername(authentication.getName()).orElseThrow();
        return ResponseEntity.ok(Map.of(
                "username", user.getUsername(),
                "role", user.getRole(),
                "accountNumber", user.getAccountNumber() != null ? user.getAccountNumber() : "",
                "phone", user.getPhone() != null ? user.getPhone() : ""
        ));
    }

    // POST /api/v1/auth/register/send-otp
    // Step 1 of verified registration: client submits phone, backend sends OTP.
    // Works for NEW phones only — if phone already exists, still returns 200 (don't reveal).
    @PostMapping("/register/send-otp")
    public ResponseEntity<?> registerSendOtp(@RequestBody Map<String, String> body) {
        String phone = body.get("phone");
        if (phone == null || phone.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "phone is required"));
        }
        // Don't send if phone already taken — but return generic message (no enumeration)
        if (!userRepository.existsByPhone(phone)) {
            String otp     = otpService.generateAndStore("reg:" + phone);
            String message = "Your PayLedger registration OTP is: " + otp + ". Valid for 5 minutes. Do not share.";
            smsService.send(phone, message);
            log.info("Registration OTP sent to phone ending ...{}", phone.length() > 4 ? phone.substring(phone.length() - 4) : "****");
        }
        return ResponseEntity.ok(Map.of("message", "If that number is available, an OTP has been sent."));
    }

    // POST /api/v1/auth/forgot-password
    // Step 1: user submits their registered phone number → receives a 6-digit OTP via SMS.
    // Always returns 200 with a generic message (security: don't reveal if phone exists).
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        userRepository.findByPhone(request.getPhone()).ifPresent(user -> {
            String otp     = otpService.generateAndStore(request.getPhone());
            String message = "Your PayLedger OTP is: " + otp + ". Valid for 5 minutes. Do not share.";
            smsService.send(request.getPhone(), message);
            log.info("Password reset OTP sent for user '{}'", user.getUsername());
        });
        // Always return 200 — never reveal whether the phone is registered
        return ResponseEntity.ok(Map.of("message", "If that number is registered, an OTP has been sent."));
    }

    // POST /api/v1/auth/reset-password
    // Step 2: user submits phone + OTP + new password → password is updated.
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        // Verify OTP (also consumes it — one-time use)
        if (!otpService.verifyAndConsume(request.getPhone(), request.getOtp())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Invalid or expired OTP. Please request a new one."));
        }

        User user = userRepository.findByPhone(request.getPhone()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "No account found for this phone number."));
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Password reset successfully for user '{}'", user.getUsername());

        return ResponseEntity.ok(Map.of("message", "Password reset successfully. You can now log in."));
    }

    // POST /api/v1/auth/change-password
    // For authenticated users — requires current password confirmation.
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                            Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Not authenticated"));
        }

        User user = userRepository.findByUsername(authentication.getName()).orElseThrow();

        // Verify current password before allowing change
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Current password is incorrect."));
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Password changed for user '{}'", user.getUsername());

        return ResponseEntity.ok(Map.of("message", "Password changed successfully."));
    }
}
