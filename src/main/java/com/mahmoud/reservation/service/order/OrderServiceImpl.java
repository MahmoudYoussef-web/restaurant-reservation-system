package com.mahmoud.reservation.service.order;

import com.mahmoud.reservation.dto.order.*;
import com.mahmoud.reservation.entity.MenuItem;
import com.mahmoud.reservation.entity.Order;
import com.mahmoud.reservation.entity.OrderItem;
import com.mahmoud.reservation.enums.OrderStatus;
import com.mahmoud.reservation.exception.BadRequestException;
import com.mahmoud.reservation.exception.ResourceNotFoundException;
import com.mahmoud.reservation.repository.MenuItemRepository;
import com.mahmoud.reservation.repository.OrderItemRepository;
import com.mahmoud.reservation.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final MenuItemRepository menuItemRepository;

    @Override
    public OrderResponse createOrder(CreateOrderRequest request, Long userId) {
        Order order = Order.builder()
                .tableId(request.getTableId())
                .restaurantId(request.getRestaurantId())
                .userId(userId)
                .notes(request.getNotes())
                .status(OrderStatus.PENDING)
                .build();

        Order saved = orderRepository.save(order);
        log.info("Created order {} for table {}", saved.getId(), request.getTableId());
        return toOrderResponse(saved);
    }

    @Override
    public OrderResponse addItem(Long orderId, AddOrderItemRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        MenuItem menuItem = menuItemRepository.findById(request.getMenuItemId())
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found"));

        OrderItem item = OrderItem.builder()
                .order(order)
                .menuItem(menuItem)
                .quantity(request.getQuantity())
                .unitPrice(menuItem.getPrice())
                .build();

        order.getItems().add(item);
        Order saved = orderRepository.save(order);
        log.info("Added item {} to order {}", menuItem.getName(), orderId);
        return toOrderResponse(saved);
    }

    @Override
    public OrderResponse updateStatus(Long orderId, UpdateOrderStatusRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        OrderStatus newStatus;
        try {
            newStatus = OrderStatus.valueOf(request.getStatus().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid order status");
        }

        if (order.getStatus() == OrderStatus.COMPLETED || order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("Order is already " + order.getStatus().name().toLowerCase());
        }

        order.setStatus(newStatus);
        Order saved = orderRepository.save(order);
        log.info("Order {} status changed to {}", orderId, newStatus);
        return toOrderResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        return toOrderResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByTable(Long tableId) {
        return orderRepository.findByTableIdOrderByCreatedAtDesc(tableId)
                .stream()
                .map(this::toOrderResponse)
                .toList();
    }

    private OrderResponse toOrderResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(item -> OrderItemResponse.builder()
                        .id(item.getId())
                        .menuItemId(item.getMenuItem().getId())
                        .menuItemName(item.getMenuItem().getName())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .subtotal(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                        .build())
                .toList();

        BigDecimal total = itemResponses.stream()
                .map(OrderItemResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return OrderResponse.builder()
                .id(order.getId())
                .tableId(order.getTableId())
                .restaurantId(order.getRestaurantId())
                .userId(order.getUserId())
                .status(order.getStatus().name())
                .notes(order.getNotes())
                .createdAt(order.getCreatedAt())
                .items(itemResponses)
                .total(total)
                .build();
    }
}
