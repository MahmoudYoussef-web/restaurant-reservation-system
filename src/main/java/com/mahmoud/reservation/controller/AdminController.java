package com.mahmoud.reservation.controller;

import com.mahmoud.reservation.dto.admin.*;
import com.mahmoud.reservation.dto.common.MessageResponse;
import com.mahmoud.reservation.dto.common.PageResponse;
import com.mahmoud.reservation.dto.menu.MenuCategoryRequest;
import com.mahmoud.reservation.dto.menu.MenuCategoryResponse;
import com.mahmoud.reservation.dto.menu.MenuItemRequest;
import com.mahmoud.reservation.dto.menu.MenuItemResponse;
import com.mahmoud.reservation.dto.restaurant.RestaurantResponse;
import com.mahmoud.reservation.dto.table.DiningTableResponse;
import com.mahmoud.reservation.service.admin.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    @Operation(summary = "Create restaurant")
    @PostMapping("/restaurants")
    public ResponseEntity<RestaurantResponse> createRestaurant(
            @Valid @RequestBody CreateRestaurantRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminService.createRestaurant(request));
    }

    @Operation(summary = "Update restaurant")
    @PutMapping("/restaurants/{id}")
    public ResponseEntity<RestaurantResponse> updateRestaurant(
            @PathVariable Long id,
            @Valid @RequestBody CreateRestaurantRequest request
    ) {
        return ResponseEntity.ok(adminService.updateRestaurant(id, request));
    }

    @Operation(summary = "Delete restaurant")
    @DeleteMapping("/restaurants/{id}")
    public ResponseEntity<MessageResponse> deleteRestaurant(@PathVariable Long id) {
        adminService.deleteRestaurant(id);
        return ResponseEntity.ok(new MessageResponse("Restaurant deleted successfully"));
    }

    @Operation(summary = "Create dining table")
    @PostMapping("/tables")
    public ResponseEntity<DiningTableResponse> createDiningTable(
            @Valid @RequestBody CreateDiningTableRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminService.createDiningTable(request));
    }

    @Operation(summary = "Update dining table")
    @PutMapping("/tables/{id}")
    public ResponseEntity<DiningTableResponse> updateDiningTable(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDiningTableRequest request
    ) {
        return ResponseEntity.ok(adminService.updateDiningTable(id, request));
    }

    @Operation(summary = "Delete dining table")
    @DeleteMapping("/tables/{id}")
    public ResponseEntity<MessageResponse> deleteDiningTable(@PathVariable Long id) {
        adminService.deleteDiningTable(id);
        return ResponseEntity.ok(new MessageResponse("Dining table deleted successfully"));
    }

    @Operation(summary = "Update table status")
    @PutMapping("/tables/{id}/status")
    public ResponseEntity<DiningTableResponse> updateTableStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTableStatusRequest request
    ) {
        return ResponseEntity.ok(adminService.updateTableStatus(id, request));
    }

    @Operation(summary = "Get all restaurants (paginated)")
    @GetMapping("/restaurants")
    public ResponseEntity<PageResponse<RestaurantResponse>> getAllRestaurants(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(adminService.getAllRestaurants(page, size));
    }

    @Operation(summary = "Get tables by restaurant (paginated)")
    @GetMapping("/restaurants/{id}/tables")
    public ResponseEntity<PageResponse<DiningTableResponse>> getTablesByRestaurant(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(adminService.getTablesByRestaurant(id, page, size));
    }

    @Operation(summary = "Assign OWNER role to user")
    @PutMapping("/users/{userId}/role")
    public ResponseEntity<MessageResponse> assignOwnerRole(@PathVariable Long userId) {
        return ResponseEntity.ok(adminService.assignOwnerRole(userId));
    }

    @Operation(summary = "Admin cancel any reservation")
    @DeleteMapping("/reservations/{id}")
    public ResponseEntity<MessageResponse> cancelReservation(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.cancelReservationByAdmin(id));
    }

    @Operation(summary = "Approve reservation")
    @PutMapping("/reservations/{id}/approve")
    public ResponseEntity<MessageResponse> approveReservation(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.approveReservation(id));
    }

    @Operation(summary = "Reject reservation")
    @PutMapping("/reservations/{id}/reject")
    public ResponseEntity<MessageResponse> rejectReservation(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.rejectReservation(id));
    }

    @Operation(summary = "Create menu category")
    @PostMapping("/categories")
    public ResponseEntity<MenuCategoryResponse> createCategory(
            @Valid @RequestBody MenuCategoryRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminService.createCategory(request));
    }

    @Operation(summary = "Update menu category")
    @PutMapping("/categories/{id}")
    public ResponseEntity<MenuCategoryResponse> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody MenuCategoryRequest request
    ) {
        return ResponseEntity.ok(adminService.updateCategory(id, request));
    }

    @Operation(summary = "Delete menu category")
    @DeleteMapping("/categories/{id}")
    public ResponseEntity<MessageResponse> deleteCategory(@PathVariable Long id) {
        adminService.deleteCategory(id);
        return ResponseEntity.ok(new MessageResponse("Category deleted successfully"));
    }

    @Operation(summary = "Create menu item")
    @PostMapping("/menu-items")
    public ResponseEntity<MenuItemResponse> createMenuItem(
            @Valid @RequestBody MenuItemRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminService.createMenuItem(request));
    }

    @Operation(summary = "Update menu item")
    @PutMapping("/menu-items/{id}")
    public ResponseEntity<MenuItemResponse> updateMenuItem(
            @PathVariable Long id,
            @Valid @RequestBody MenuItemRequest request
    ) {
        return ResponseEntity.ok(adminService.updateMenuItem(id, request));
    }

    @Operation(summary = "Delete menu item")
    @DeleteMapping("/menu-items/{id}")
    public ResponseEntity<MessageResponse> deleteMenuItem(@PathVariable Long id) {
        adminService.deleteMenuItem(id);
        return ResponseEntity.ok(new MessageResponse("Menu item deleted successfully"));
    }
}