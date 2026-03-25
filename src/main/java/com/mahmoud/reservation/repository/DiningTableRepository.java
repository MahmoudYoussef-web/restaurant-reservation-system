package com.mahmoud.reservation.repository;

import com.mahmoud.reservation.entity.DiningTable;
import com.mahmoud.reservation.enums.ReservationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface DiningTableRepository extends JpaRepository<DiningTable, Long> {


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM DiningTable t WHERE t.id = :id")
    Optional<DiningTable> findWithLockById(Long id);


    boolean existsByRestaurantIdAndTableNumber(Long restaurantId, Integer tableNumber);

    Page<DiningTable> findByRestaurantId(Long restaurantId, Pageable pageable);

    List<DiningTable> findByRestaurantId(Long restaurantId);


    @Query("""
        SELECT t FROM DiningTable t
        WHERE t.restaurant.id = :restaurantId
        AND NOT EXISTS (
            SELECT 1 FROM Reservation r
            WHERE r.table.id = t.id
            AND r.startTime < :endTime
            AND r.endTime > :startTime
            AND r.status IN :statuses
        )
    """)
    List<DiningTable> findAvailableTables(
            @Param("restaurantId") Long restaurantId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime,
            @Param("statuses") List<ReservationStatus> statuses
    );
}