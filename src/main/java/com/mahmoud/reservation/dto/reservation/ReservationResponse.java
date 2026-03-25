package com.mahmoud.reservation.dto.reservation;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationResponse {

    private Long id;
    private Long tableId;
    private Long restaurantId;
    private Instant startTime;
    private Instant endTime;
    private String status;
    private Integer numberOfGuests;
}