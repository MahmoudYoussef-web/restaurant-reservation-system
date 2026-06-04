package com.mahmoud.reservation.repository;

import com.mahmoud.reservation.entity.MenuCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MenuCategoryRepository extends JpaRepository<MenuCategory, Long> {

    boolean existsByName(String name);

    Optional<MenuCategory> findByName(String name);
}
