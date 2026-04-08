package com.darshan.payment_ledger.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

// JwtUtil is a UTILITY class — pure functions, no state.
// Responsible for 3 things only:
//   1. Generate a JWT token for a logged-in user
//   2. Validate a token (check signature + expiry)
//   3. Extract data (username, role) from a token
//
// @Component = Spring manages this as a singleton bean.
// Any class that needs JwtUtil injects it via constructor.

@Component
public class JwtUtil {

    // Secret key for signing/verifying tokens.
    // MUST be at least 256 bits (32 characters) for HS256 algorithm.
    // Stored in application.properties — NEVER hardcode in source code.
    // In production this would come from environment variable or secrets manager (AWS Secrets Manager etc.)
    @Value("${jwt.secret}")
    private String secret;

    // Token validity: 24 hours in milliseconds
    // 24 * 60 * 60 * 1000 = 86400000
    @Value("${jwt.expiration}")
    private long expirationMs;

    // Converts the secret string into a cryptographic key object
    // Keys.hmacShaKeyFor() validates the key length and returns a SecretKey
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    // GENERATE TOKEN
    // Called in AuthController after successful login
    // Embeds: subject (username), role claim, issued-at, expiration
    // Signs with HMAC-SHA256 using our secret key
    public String generateToken(String username, String role) {
        return Jwts.builder()
                .subject(username)                              // who this token belongs to
                .claim("role", role)                           // custom claim: CUSTOMER/BACKOFFICE/ADMIN
                .issuedAt(new Date())                          // when token was created
                .expiration(new Date(System.currentTimeMillis() + expirationMs))  // when it expires
                .signWith(getSigningKey())                     // sign with our secret
                .compact();                                    // build the final string
    }

    // EXTRACT USERNAME
    // Called in JwtFilter to find out who made the request
    public String extractUsername(String token) {
        return getClaims(token).getSubject();
    }

    // EXTRACT ROLE
    // Called in JwtFilter to load correct authorities
    public String extractRole(String token) {
        return getClaims(token).get("role", String.class);
    }

    // VALIDATE TOKEN
    // Returns true if token signature is valid AND token is not expired
    // Returns false (or throws) if token is tampered or expired
    public boolean validateToken(String token) {
        try {
            getClaims(token); // throws if invalid or expired
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // Parse and verify the token — returns all claims inside it
    // Throws JwtException if signature doesn't match our secret
    // Throws ExpiredJwtException if expiration date has passed
    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())   // verify signature using our secret
                .build()
                .parseSignedClaims(token)      // parse + validate
                .getPayload();                 // get the body (claims)
    }
}
