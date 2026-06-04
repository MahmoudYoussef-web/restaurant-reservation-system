package com.mahmoud.reservation.repository;

import com.mahmoud.reservation.entity.Order;
import com.mahmoud.reservation.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByTableIdOrderByCreatedAtDesc(Long tableId);

    List<Order> findByRestaurantIdOrderByCreatedAtDesc(Long restaurantId);

    boolean existsByTableIdAndStatusIn(Long tableId, List<OrderStatus> statuses);
}
