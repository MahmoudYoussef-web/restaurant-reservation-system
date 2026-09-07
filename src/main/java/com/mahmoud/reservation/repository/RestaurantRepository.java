package com.mahmoud.reservation.repository;

import com.mahmoud.reservation.entity.Restaurant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    Page<Restaurant> findByNameContainingIgnoreCaseOrLocationContainingIgnoreCase(
            String name, String location, Pageable pageable);

    Page<Restaurant> findByNameContainingIgnoreCaseOrLocationContainingIgnoreCaseOrCuisineContainingIgnoreCase(
            String name, String location, String cuisine, Pageable pageable);
}