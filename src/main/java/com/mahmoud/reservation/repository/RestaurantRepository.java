package com.mahmoud.reservation.repository;

import com.mahmoud.reservation.entity.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

    boolean existsByName(String name);
}