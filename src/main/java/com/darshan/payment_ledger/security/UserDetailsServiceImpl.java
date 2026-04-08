package com.darshan.payment_ledger.security;

import com.darshan.payment_ledger.entity.User;
import com.darshan.payment_ledger.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

// UserDetailsService is a Spring Security INTERFACE.
// Spring calls loadUserByUsername() during authentication.
// Our job: find the User in DB and wrap it in a UserDetails object.
//
// UserDetails is what Spring Security works with internally.
// It carries: username, password (hashed), authorities (roles), enabled status.
// Spring then compares the provided password with the stored hash using BCrypt.

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    // Called automatically by Spring Security's AuthenticationManager during login
    // Also called by JwtFilter on every authenticated request
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        // "ROLE_" prefix is Spring Security convention.
        // hasRole("CUSTOMER") checks for authority "ROLE_CUSTOMER".
        // We add the prefix here so the rest of the code uses clean role names.
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + user.getRole().name());

        // org.springframework.security.core.userdetails.User (Spring's class, not our entity)
        // Takes: username, hashed password, list of authorities
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),         // already BCrypt hashed — Spring verifies, doesn't re-hash
                user.isEnabled(),           // account active?
                true,                       // accountNonExpired
                true,                       // credentialsNonExpired
                true,                       // accountNonLocked
                List.of(authority)          // roles/permissions
        );
    }
}
