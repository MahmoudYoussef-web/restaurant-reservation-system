package com.mahmoud.reservation.repository;

import com.mahmoud.reservation.entity.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {

    List<MenuItem> findByCategoryIdOrderByName(Long categoryId);
}
