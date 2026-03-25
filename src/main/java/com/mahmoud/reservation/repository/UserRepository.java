package com.mahmoud.reservation.repository;

import com.mahmoud.reservation.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    @Query("""
    SELECT u FROM User u
    LEFT JOIN FETCH u.userRoles ur
    LEFT JOIN FETCH ur.role
    WHERE u.email = :email
""")
    Optional<User> findByEmailWithRoles(String email);

    boolean existsByEmail(String email);
}