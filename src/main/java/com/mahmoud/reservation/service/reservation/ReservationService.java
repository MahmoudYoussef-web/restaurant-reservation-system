package com.mahmoud.reservation.service.reservation;

import com.mahmoud.reservation.dto.reservation.CreateReservationRequest;
import com.mahmoud.reservation.dto.reservation.ReservationResponse;

import java.util.List;

public interface ReservationService {

    ReservationResponse createReservation(CreateReservationRequest request, Long userId);

    List<ReservationResponse> getUserReservations(Long userId);

    void cancelReservation(Long reservationId, Long userId);

    ReservationResponse getReservationById(Long reservationId, Long userId);
}