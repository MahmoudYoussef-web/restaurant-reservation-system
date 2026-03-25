package com.mahmoud.reservation.dto.reservation;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateReservationRequest {

    @NotNull
    private Long tableId;

    @NotNull
    private Instant startTime;

    @NotNull
    private Instant endTime;

    @NotNull
    @Min(1)
    private Integer numberOfGuests;

    private String specialRequest;

    public void validate() {
        if (endTime.isBefore(startTime)) {
            throw new IllegalArgumentException("Invalid time range");
        }
    }
}