package com.mahmoud.reservation.service.reservation;

import com.mahmoud.reservation.dto.reservation.CreateReservationRequest;
import com.mahmoud.reservation.dto.reservation.ReservationResponse;
import com.mahmoud.reservation.dto.table.DiningTableResponse;

import java.time.Instant;
import java.util.List;

public interface ReservationService {

    ReservationResponse createReservation(CreateReservationRequest request, Long userId);

    List<ReservationResponse> getUserReservations(Long userId);

    void cancelReservation(Long reservationId, Long userId);

    ReservationResponse getReservationById(Long reservationId, Long userId);

    List<DiningTableResponse> getAvailableTables(Long restaurantId, Instant startTime, Instant endTime);
}