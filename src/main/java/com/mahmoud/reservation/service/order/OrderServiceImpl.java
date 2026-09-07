package com.mahmoud.reservation.service.order;

import com.mahmoud.reservation.dto.order.*;
import com.mahmoud.reservation.entity.DiningTable;
import com.mahmoud.reservation.entity.MenuItem;
import com.mahmoud.reservation.entity.Order;
import com.mahmoud.reservation.entity.OrderItem;
import com.mahmoud.reservation.entity.Restaurant;
import com.mahmoud.reservation.enums.OrderStatus;
import com.mahmoud.reservation.exception.BadRequestException;
import com.mahmoud.reservation.exception.ResourceNotFoundException;
import com.mahmoud.reservation.repository.DiningTableRepository;
import com.mahmoud.reservation.repository.MenuItemRepository;
import com.mahmoud.reservation.repository.OrderItemRepository;
import com.mahmoud.reservation.repository.OrderRepository;
import com.mahmoud.reservation.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(OrderStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(OrderStatus.PENDING, Set.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED));
        ALLOWED_TRANSITIONS.put(OrderStatus.CONFIRMED, Set.of(OrderStatus.PREPARING, OrderStatus.CANCELLED));
        ALLOWED_TRANSITIONS.put(OrderStatus.PREPARING, Set.of(OrderStatus.READY, OrderStatus.CANCELLED));
        ALLOWED_TRANSITIONS.put(OrderStatus.READY, Set.of(OrderStatus.SERVED, OrderStatus.CANCELLED));
        ALLOWED_TRANSITIONS.put(OrderStatus.SERVED, Set.of(OrderStatus.COMPLETED, OrderStatus.CANCELLED));
        ALLOWED_TRANSITIONS.put(OrderStatus.COMPLETED, Set.of());
        ALLOWED_TRANSITIONS.put(OrderStatus.CANCELLED, Set.of());
    }

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final MenuItemRepository menuItemRepository;
    private final DiningTableRepository diningTableRepository;
    private final RestaurantRepository restaurantRepository;

    @Override
    public OrderResponse createOrder(CreateOrderRequest request, Long userId) {
        Restaurant restaurant = restaurantRepository.findById(request.getRestaurantId())
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found"));

        DiningTable table = diningTableRepository.findById(request.getTableId())
                .orElseThrow(() -> new ResourceNotFoundException("Table not found"));

        if (!table.getRestaurant().getId().equals(restaurant.getId())) {
            throw new BadRequestException("Table does not belong to this restaurant");
        }

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

        if (order.getStatus() == OrderStatus.COMPLETED || order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("Cannot modify a " + order.getStatus().name().toLowerCase() + " order");
        }

        if (request.getQuantity() < 1) {
            throw new BadRequestException("Quantity must be at least 1");
        }

        MenuItem menuItem = menuItemRepository.findById(request.getMenuItemId())
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found"));

        if (!menuItem.isAvailable()) {
            throw new BadRequestException("Menu item is currently unavailable");
        }

        OrderItem existing = order.getItems().stream()
                .filter(i -> i.getMenuItem().getId().equals(menuItem.getId()))
                .findFirst()
                .orElse(null);

        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + request.getQuantity());
        } else {
            OrderItem item = OrderItem.builder()
                    .order(order)
                    .menuItem(menuItem)
                    .quantity(request.getQuantity())
                    .unitPrice(menuItem.getPrice())
                    .build();
            order.getItems().add(item);
        }

        Order saved = orderRepository.save(order);
        log.info("Added item {} to order {}", menuItem.getName(), orderId);
        return toOrderResponse(saved);
    }

    @Override
    public OrderResponse updateStatus(Long orderId, UpdateOrderStatusRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (request.getStatus() == null || request.getStatus().isBlank()) {
            throw new BadRequestException("Status is required");
        }

        OrderStatus newStatus;
        try {
            newStatus = OrderStatus.valueOf(request.getStatus().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid order status");
        }

        if (!ALLOWED_TRANSITIONS.getOrDefault(order.getStatus(), Set.of()).contains(newStatus)) {
            throw new BadRequestException(
                    "Cannot transition order from " + order.getStatus().name() + " to " + newStatus.name());
        }

        order.setStatus(newStatus);
        Order saved = orderRepository.save(order);
        log.info("Order {} status changed to {}", orderId, newStatus);
        return toOrderResponse(saved);
    }

    @Override
    public OrderResponse cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getStatus() == OrderStatus.COMPLETED || order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("Order is already " + order.getStatus().name().toLowerCase());
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order saved = orderRepository.save(order);
        log.info("Order {} cancelled", orderId);
        return toOrderResponse(saved);
    }

    @Override
    public OrderResponse removeItem(Long orderId, Long itemId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getStatus() == OrderStatus.COMPLETED || order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("Cannot modify a " + order.getStatus().name().toLowerCase() + " order");
        }

        OrderItem item = order.getItems().stream()
                .filter(i -> itemId.equals(i.getId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Order item not found"));

        order.getItems().remove(item);
        Order saved = orderRepository.save(order);
        log.info("Removed item {} from order {}", itemId, orderId);
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
