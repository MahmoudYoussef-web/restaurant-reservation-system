package com.mahmoud.reservation.controller;

import com.mahmoud.reservation.dto.common.MessageResponse;
import com.mahmoud.reservation.dto.order.*;
import com.mahmoud.reservation.exception.UnauthorizedException;
import com.mahmoud.reservation.security.user.ShopUserDetails;
import com.mahmoud.reservation.service.order.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "Create order")
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        Long userId = getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(request, userId));
    }

    @Operation(summary = "Add item to order")
    @PostMapping("/{id}/items")
    public ResponseEntity<OrderResponse> addItem(
            @PathVariable Long id,
            @Valid @RequestBody AddOrderItemRequest request
    ) {
        return ResponseEntity.ok(orderService.addItem(id, request));
    }

    @Operation(summary = "Update order status")
    @PutMapping("/{id}/status")
    public ResponseEntity<OrderResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderStatusRequest request
    ) {
        return ResponseEntity.ok(orderService.updateStatus(id, request));
    }

    @Operation(summary = "Get order by ID")
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    @Operation(summary = "Get orders by table")
    @GetMapping("/table/{tableId}")
    public ResponseEntity<List<OrderResponse>> getOrdersByTable(@PathVariable Long tableId) {
        return ResponseEntity.ok(orderService.getOrdersByTable(tableId));
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof ShopUserDetails userDetails)) {
            throw new UnauthorizedException("Unauthorized");
        }
        return userDetails.getId();
    }
}
