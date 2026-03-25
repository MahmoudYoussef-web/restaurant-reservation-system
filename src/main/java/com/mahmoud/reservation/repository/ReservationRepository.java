package com.mahmoud.reservation.repository;

import com.mahmoud.reservation.entity.Reservation;
import com.mahmoud.reservation.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    @Query("""
        SELECT r FROM Reservation r
        WHERE r.table.id = :tableId
        AND r.startTime < :endTime
        AND r.endTime > :startTime
        AND r.status IN :statuses
    """)
    List<Reservation> findConflictingReservations(
            Long tableId,
            Instant startTime,
            Instant endTime,
            List<ReservationStatus> statuses
    );

    List<Reservation> findByUserId(Long userId);

    List<Reservation> findByTableId(Long tableId);
}