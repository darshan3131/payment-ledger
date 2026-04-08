package com.darshan.payment_ledger.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// JwtFilter runs on EVERY HTTP request — before any controller is called.
// It checks if the request carries a valid JWT token.
// If yes → sets the authenticated user in SecurityContext → request proceeds.
// If no → SecurityContext stays empty → SecurityConfig rules kick in.
//          (public endpoints pass through; protected endpoints get 401 Unauthorized)
//
// OncePerRequestFilter: Spring guarantees this filter runs EXACTLY ONCE per request.
// Without this, some filters can run multiple times (e.g. during error forwarding).

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // Step 1: Read the Authorization header
        // Expected format: "Bearer eyJhbGciOiJIUzI1NiJ9.eyJ..."
        final String authHeader = request.getHeader("Authorization");

        // If header is missing or doesn't start with "Bearer " → skip JWT validation
        // The request continues — SecurityConfig will block protected endpoints
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Step 2: Extract the token (everything after "Bearer ")
        // "Bearer eyJhbGci..." → "eyJhbGci..."
        final String token = authHeader.substring(7);

        // Step 3: Validate token and extract username
        if (!jwtUtil.validateToken(token)) {
            log.warn("Invalid or expired JWT token for request: {}", request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        final String username = jwtUtil.extractUsername(token);

        // Step 4: Only set authentication if not already authenticated
        // (prevents re-authenticating on the same request)
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // Load user details from DB (gets username, hashed password, authorities)
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            // Step 5: Create authentication token
            // UsernamePasswordAuthenticationToken = Spring's way of saying "this user is authenticated"
            // 3-arg constructor = authenticated (2-arg constructor = not yet authenticated)
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,          // principal (who is this user)
                            null,                 // credentials (null — we already verified via JWT)
                            userDetails.getAuthorities()  // roles: [ROLE_CUSTOMER] etc.
                    );

            // Attach request details (IP address, session ID) — useful for audit logging
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            // Step 6: Set in SecurityContext
            // This is what @PreAuthorize and hasRole() checks read from
            // Thread-local: each request thread has its own SecurityContext
            SecurityContextHolder.getContext().setAuthentication(authToken);

            log.debug("Authenticated user '{}' with role '{}' for {}",
                    username, jwtUtil.extractRole(token), request.getRequestURI());
        }

        // Step 7: Continue to next filter / controller
        filterChain.doFilter(request, response);
    }
}
