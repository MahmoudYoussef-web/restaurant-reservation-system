package com.mahmoud.reservation.controller;

import com.mahmoud.reservation.dto.table.DiningTableResponse;
import com.mahmoud.reservation.service.reservation.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/restaurants")
public class RestaurantController {

    private final ReservationService reservationService;

    @GetMapping("/{id}/available-tables")
    public ResponseEntity<List<DiningTableResponse>> getAvailableTables(
            @PathVariable Long id,
            @RequestParam Instant startTime,
            @RequestParam Instant endTime
    ) {
        return ResponseEntity.ok(
                reservationService.getAvailableTables(id, startTime, endTime)
        );
    }
}