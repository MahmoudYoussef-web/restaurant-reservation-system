package com.mahmoud.reservation.repository;

import com.mahmoud.reservation.entity.User;
import com.mahmoud.reservation.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRoleRepository extends JpaRepository<UserRole, Long> {

    List<UserRole> findByUser(User user);

    List<UserRole> findByUserId(Long userId);
}