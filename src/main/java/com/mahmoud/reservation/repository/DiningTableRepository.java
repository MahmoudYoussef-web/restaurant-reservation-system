package com.mahmoud.reservation.repository;

import com.mahmoud.reservation.entity.DiningTable;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;
import java.util.List;

public interface DiningTableRepository extends JpaRepository<DiningTable, Long> {

    Optional<DiningTable> findById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<DiningTable> findWithLockById(Long id);

    Page<DiningTable> findByRestaurantId(Long restaurantId, Pageable pageable);

    boolean existsByRestaurantIdAndTableNumber(Long restaurantId, Integer tableNumber);

    List<DiningTable> findByRestaurantId(Long restaurantId);
}