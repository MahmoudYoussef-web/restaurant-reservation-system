package com.mahmoud.reservation.repository;

import com.mahmoud.reservation.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    Page<Review> findByRestaurantIdOrderByCreatedAtDesc(Long restaurantId, Pageable pageable);

    List<Review> findByUserIdAndRestaurantId(Long userId, Long restaurantId);
}
