package com.darshan.payment_ledger.config;

import com.darshan.payment_ledger.security.JwtFilter;
import com.darshan.payment_ledger.security.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

// SecurityConfig is the MASTER configuration for Spring Security.
// Every access rule, every filter registration, every auth mechanism is defined here.
//
// @EnableWebSecurity   = activates Spring Security for this app
// @EnableMethodSecurity = enables @PreAuthorize on individual controller methods

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final UserDetailsServiceImpl userDetailsService;

    // THE MOST IMPORTANT BEAN — defines what is allowed and what is blocked
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> {})
            // Disable CSRF (Cross-Site Request Forgery) protection.
            // CSRF attacks require cookies/sessions. We use JWT (stateless) — no sessions.
            // CSRF protection on stateless APIs causes legitimate requests to be rejected.
            .csrf(AbstractHttpConfigurer::disable)

            // Stateless session: Spring Security will NOT create or use HTTP sessions.
            // Every request must carry its own JWT. No "remember me" cookies.
            // This is correct for REST APIs and microservices.
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // ACCESS RULES — checked in order, first match wins
            .authorizeHttpRequests(auth -> auth

                // PUBLIC endpoints — no token required
                .requestMatchers("/api/v1/auth/**").permitAll()                              // login, register
                .requestMatchers("/actuator/health").permitAll()                             // Render health checks
                .requestMatchers("/").permitAll()                                            // portal selector HTML
                .requestMatchers("/customer/**", "/backoffice/**", "/admin/**").permitAll()  // static HTML

                // CUSTOMER endpoints — any authenticated user (customer, backoffice, admin)
                .requestMatchers(HttpMethod.GET,  "/api/v1/accounts/my").authenticated()
                .requestMatchers(HttpMethod.GET,  "/api/v1/accounts/number/**").authenticated()
                .requestMatchers(HttpMethod.GET,  "/api/v1/accounts/*/ledger").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/v1/transactions").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/v1/transactions/request-otp").authenticated()
                .requestMatchers(HttpMethod.GET,  "/api/v1/transactions/*").authenticated()

                // BACKOFFICE endpoints — only BACKOFFICE or ADMIN role
                .requestMatchers(HttpMethod.GET,   "/api/v1/accounts/available-customers").hasAnyRole("BACKOFFICE", "ADMIN")
                .requestMatchers(HttpMethod.GET,   "/api/v1/accounts").hasAnyRole("BACKOFFICE", "ADMIN")
                .requestMatchers(HttpMethod.POST,  "/api/v1/accounts").hasAnyRole("BACKOFFICE", "ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/v1/accounts/*/status").hasAnyRole("BACKOFFICE", "ADMIN")
                .requestMatchers(HttpMethod.GET,   "/api/v1/transactions").hasAnyRole("BACKOFFICE", "ADMIN")
                .requestMatchers(HttpMethod.GET,   "/api/v1/transactions/account/**").hasAnyRole("BACKOFFICE", "ADMIN")
                .requestMatchers(HttpMethod.GET,   "/api/v1/transactions/*/ledger").hasAnyRole("BACKOFFICE", "ADMIN")
                .requestMatchers(HttpMethod.POST,  "/api/v1/transactions/deposit").hasAnyRole("BACKOFFICE", "ADMIN")
                .requestMatchers(HttpMethod.POST,  "/api/v1/transactions/withdraw").hasAnyRole("BACKOFFICE", "ADMIN")
                .requestMatchers(HttpMethod.POST,  "/api/v1/transactions/*/reverse").hasAnyRole("BACKOFFICE", "ADMIN")
                // GET customers list (for account creation dropdown) — BACKOFFICE/ADMIN
                .requestMatchers(HttpMethod.GET,   "/api/v1/users").hasRole("ADMIN")

                // SUPPORT TICKETS
                // Customer: create + view own tickets
                .requestMatchers(HttpMethod.POST, "/api/v1/support").authenticated()
                .requestMatchers(HttpMethod.GET,  "/api/v1/support/my").authenticated()
                // Backoffice/Admin: view all + update
                .requestMatchers(HttpMethod.GET,   "/api/v1/support").hasAnyRole("BACKOFFICE", "ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/v1/support/*").hasAnyRole("BACKOFFICE", "ADMIN")

                // ADMIN only
                .requestMatchers("/api/v1/analytics/**").hasRole("ADMIN")
                .requestMatchers("/api/v1/users/**").hasRole("ADMIN")

                // Everything else requires authentication
                .anyRequest().authenticated()
            )

            // Register our JwtFilter BEFORE Spring's built-in UsernamePasswordAuthenticationFilter.
            // Why before? Spring's filter tries form-based login (username/password in request body).
            // We don't use form login — we use JWT. Our filter must run first to set the SecurityContext.
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // PasswordEncoder: BCrypt algorithm for hashing passwords.
    // BCrypt is slow by design (cost factor = 10 rounds by default).
    // Slow = harder to brute-force if DB is stolen.
    // Never use MD5, SHA1, or SHA256 for passwords — they're too fast.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // AuthenticationProvider: connects Spring Security's auth system to our DB.
    // DaoAuthenticationProvider = "Database Authentication Object Provider"
    //   - Uses UserDetailsService to load user from DB
    //   - Uses PasswordEncoder to verify the provided password against the stored hash
    @Bean
    public AuthenticationProvider authenticationProvider() {
        // Spring Security 6.4+: UserDetailsService passed to constructor, not setUserDetailsService()
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    // AuthenticationManager: Spring Security's central auth coordinator.
    // AuthController injects this and calls authenticate(username, password).
    // AuthenticationManager delegates to our AuthenticationProvider.
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    // CORS — allow the Vercel-hosted frontends to call this API.
    // Without this, browsers block cross-origin requests from *.vercel.app → *.onrender.com.
    @Bean
    public org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource() {
        org.springframework.web.cors.CorsConfiguration config = new org.springframework.web.cors.CorsConfiguration();

        config.setAllowedOrigins(java.util.List.of(
                "https://payment-ledger-6rqk.vercel.app",   // customer portal
                "https://payment-ledger-5qx9.vercel.app",   // backoffice portal
                "https://payment-ledger-bt73.vercel.app"    // admin portal
        ));

        config.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(java.util.List.of("*"));
        config.setAllowCredentials(true);

        org.springframework.web.cors.UrlBasedCorsConfigurationSource source =
                new org.springframework.web.cors.UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", config);

        return source;
    }
}
