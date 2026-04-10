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

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final UserDetailsServiceImpl userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> {}) // ✅ ENABLE CORS
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth

                        // PUBLIC
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/").permitAll()
                        .requestMatchers("/customer/**", "/backoffice/**", "/admin/**").permitAll()

                        // CUSTOMER
                        .requestMatchers(HttpMethod.GET,  "/api/v1/accounts/my").authenticated()
                        .requestMatchers(HttpMethod.GET,  "/api/v1/accounts/number/**").authenticated()
                        .requestMatchers(HttpMethod.GET,  "/api/v1/accounts/*/ledger").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/transactions").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/transactions/request-otp").authenticated()
                        .requestMatchers(HttpMethod.GET,  "/api/v1/transactions/*").authenticated()

                        // BACKOFFICE
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
                        .requestMatchers(HttpMethod.GET,   "/api/v1/users").hasRole("ADMIN")

                        // SUPPORT
                        .requestMatchers(HttpMethod.POST, "/api/v1/support").authenticated()
                        .requestMatchers(HttpMethod.GET,  "/api/v1/support/my").authenticated()
                        .requestMatchers(HttpMethod.GET,   "/api/v1/support").hasAnyRole("BACKOFFICE", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/support/*").hasAnyRole("BACKOFFICE", "ADMIN")

                        // ADMIN
                        .requestMatchers("/api/v1/analytics/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/users/**").hasRole("ADMIN")

                        .anyRequest().authenticated()
                )

                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    // ✅ CORS CONFIG (correct placement)
    @Bean
    public org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource() {
        org.springframework.web.cors.CorsConfiguration config = new org.springframework.web.cors.CorsConfiguration();

        config.setAllowedOrigins(java.util.List.of(
                "https://payment-ledger-6rqk.vercel.app"
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