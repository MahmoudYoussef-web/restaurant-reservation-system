package com.mahmoud.reservation.controller;

import com.mahmoud.reservation.dto.admin.CreateDiningTableRequest;
import com.mahmoud.reservation.dto.admin.CreateRestaurantRequest;
import com.mahmoud.reservation.dto.common.PageResponse;
import com.mahmoud.reservation.dto.restaurant.RestaurantResponse;
import com.mahmoud.reservation.dto.table.DiningTableResponse;
import com.mahmoud.reservation.service.admin.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @PostMapping("/restaurants")
    public ResponseEntity<RestaurantResponse> createRestaurant(
            @Valid @RequestBody CreateRestaurantRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminService.createRestaurant(request));
    }

    @PostMapping("/tables")
    public ResponseEntity<DiningTableResponse> createDiningTable(
            @Valid @RequestBody CreateDiningTableRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminService.createDiningTable(request));
    }

    @GetMapping("/restaurants")
    public ResponseEntity<PageResponse<RestaurantResponse>> getAllRestaurants(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(adminService.getAllRestaurants(page, size));
    }

    @GetMapping("/restaurants/{id}/tables")
    public ResponseEntity<PageResponse<DiningTableResponse>> getTablesByRestaurant(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(adminService.getTablesByRestaurant(id, page, size));
    }
}