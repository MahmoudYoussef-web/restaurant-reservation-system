package com.mahmoud.reservation.controller;

import com.mahmoud.reservation.dto.common.PageResponse;
import com.mahmoud.reservation.dto.restaurant.RestaurantResponse;
import com.mahmoud.reservation.dto.table.DiningTableResponse;
import com.mahmoud.reservation.service.reservation.ReservationService;
import com.mahmoud.reservation.service.restaurant.RestaurantService;
import io.swagger.v3.oas.annotations.Operation;
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
    private final RestaurantService restaurantService;

    @Operation(summary = "Browse restaurants (public, searchable, paginated)")
    @GetMapping
    public ResponseEntity<PageResponse<RestaurantResponse>> browseRestaurants(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(restaurantService.browseRestaurants(search, page, size));
    }

    @Operation(summary = "Get restaurant details (public)")
    @GetMapping("/{id}")
    public ResponseEntity<RestaurantResponse> getRestaurantById(@PathVariable Long id) {
        return ResponseEntity.ok(restaurantService.getRestaurantById(id));
    }

    @Operation(summary = "Get available tables for a time range")
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
