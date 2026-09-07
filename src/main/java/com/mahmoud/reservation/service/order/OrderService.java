package com.mahmoud.reservation.service.order;

import com.mahmoud.reservation.dto.order.*;

import java.util.List;

public interface OrderService {

    OrderResponse createOrder(CreateOrderRequest request, Long userId);

    OrderResponse addItem(Long orderId, AddOrderItemRequest request);

    OrderResponse updateStatus(Long orderId, UpdateOrderStatusRequest request);

    OrderResponse cancelOrder(Long orderId);

    OrderResponse removeItem(Long orderId, Long itemId);

    OrderResponse getOrderById(Long orderId);

    List<OrderResponse> getOrdersByTable(Long tableId);
}
