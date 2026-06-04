package com.mahmoud.reservation.mapper;

import com.mahmoud.reservation.dto.reservation.ReservationResponse;
import com.mahmoud.reservation.entity.DiningTable;
import com.mahmoud.reservation.entity.Reservation;
import com.mahmoud.reservation.entity.Restaurant;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class ReservationMapper {

    public ReservationResponse toResponse(Reservation reservation) {
        if (reservation == null) {
            return null;
        }

        DiningTable table = reservation.getTable();
        Restaurant restaurant = table != null ? table.getRestaurant() : null;

        Long tableId = table != null ? table.getId() : null;
        Long restaurantId = restaurant != null ? restaurant.getId() : null;

        return ReservationResponse.builder()
                .id(reservation.getId())
                .tableId(tableId)
                .restaurantId(restaurantId)
                .startTime(reservation.getStartTime())
                .endTime(reservation.getEndTime())
                .status(reservation.getStatus() != null ? reservation.getStatus().name() : null)
                .numberOfGuests(reservation.getNumberOfGuests())
                .build();
    }

    public List<ReservationResponse> toResponseList(List<Reservation> reservations) {
        if (reservations == null || reservations.isEmpty()) {
            return Collections.emptyList();
        }

        return reservations.stream()
                .filter(Objects::nonNull)
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
}