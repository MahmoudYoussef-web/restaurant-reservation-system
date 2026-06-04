package com.mahmoud.reservation.controller;

import com.mahmoud.reservation.dto.common.MessageResponse;
import com.mahmoud.reservation.dto.common.PageResponse;
import com.mahmoud.reservation.dto.reservation.CreateReservationRequest;
import com.mahmoud.reservation.dto.reservation.ReservationResponse;
import com.mahmoud.reservation.exception.UnauthorizedException;
import com.mahmoud.reservation.security.user.ShopUserDetails;
import com.mahmoud.reservation.service.reservation.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    @Operation(summary = "Create reservation")
    @ApiResponse(responseCode = "201", description = "Reservation created successfully")
    @PostMapping
    public ResponseEntity<ReservationResponse> createReservation(
            @Valid @RequestBody CreateReservationRequest request
    ) {
        Long userId = getCurrentUserId();
        ReservationResponse response = reservationService.createReservation(request, userId);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get user reservations (paginated)")
    @GetMapping("/my")
    public ResponseEntity<PageResponse<ReservationResponse>> getMyReservations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(reservationService.getUserReservations(userId, page, size));
    }

    @Operation(summary = "Get reservation by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ReservationResponse> getReservationById(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(reservationService.getReservationById(id, userId));
    }

    @Operation(summary = "Cancel reservation")
    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> cancelReservation(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        reservationService.cancelReservation(id, userId);

        return ResponseEntity.ok(
                MessageResponse.builder()
                        .message("Reservation cancelled successfully")
                        .build()
        );
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof ShopUserDetails userDetails)) {
            throw new UnauthorizedException("Unauthorized");
        }

        return userDetails.getId();
    }
}