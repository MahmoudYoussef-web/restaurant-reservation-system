package com.mahmoud.reservation.repository;

import com.mahmoud.reservation.entity.Reservation;
import com.mahmoud.reservation.enums.ReservationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    @Query("""
        SELECT COUNT(r) > 0 FROM Reservation r
        WHERE r.table.id = :tableId
        AND r.startTime < :endTime
        AND r.endTime > :startTime
        AND r.status IN :statuses
    """)
    boolean existsConflict(
            @Param("tableId") Long tableId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime,
            @Param("statuses") List<ReservationStatus> statuses
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Reservation r WHERE r.id = :id")
    Optional<Reservation> findByIdWithLock(Long id);

    @Query("""
        SELECT r FROM Reservation r
        JOIN FETCH r.table t
        JOIN FETCH t.restaurant
        WHERE r.userId = :userId
    """)
    List<Reservation> findByUserIdWithDetails(Long userId);

    @Query("""
        SELECT t.id FROM Reservation r
        JOIN r.table t
        WHERE t.restaurant.id = :restaurantId
        AND r.startTime < :endTime
        AND r.endTime > :startTime
        AND r.status IN :statuses
    """)
    List<Long> findConflictingTableIds(
            Long restaurantId,
            Instant startTime,
            Instant endTime,
            List<ReservationStatus> statuses
    );
}