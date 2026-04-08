package com.darshan.payment_ledger.repository;

import com.darshan.payment_ledger.entity.User;
import com.darshan.payment_ledger.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Spring Security calls loadUserByUsername(username) — needs this query
    Optional<User> findByUsername(String username);

    Optional<User> findByPhone(String phone);

    List<User> findByRole(Role role);

    boolean existsByUsername(String username);

    boolean existsByPhone(String phone);
}
