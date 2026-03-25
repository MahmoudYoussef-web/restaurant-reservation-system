package com.mahmoud.reservation.repository;

import com.mahmoud.reservation.entity.DiningTable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DiningTableRepository extends JpaRepository<DiningTable, Long> {
    List<DiningTable> findByRestaurantId(Long restaurantId);
    List<DiningTable> findByRestaurantIdAndCapacityGreaterThanEqual(Long restaurantId, Integer capacity);
}