package com.mahmoud.reservation.service;

import com.mahmoud.reservation.dto.order.AddOrderItemRequest;
import com.mahmoud.reservation.dto.order.CreateOrderRequest;
import com.mahmoud.reservation.dto.order.OrderResponse;
import com.mahmoud.reservation.dto.order.UpdateOrderStatusRequest;
import com.mahmoud.reservation.entity.DiningTable;
import com.mahmoud.reservation.entity.MenuCategory;
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
import com.mahmoud.reservation.service.order.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderItemRepository orderItemRepository;
    @Mock
    private MenuItemRepository menuItemRepository;
    @Mock
    private DiningTableRepository diningTableRepository;
    @Mock
    private RestaurantRepository restaurantRepository;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Restaurant restaurant;
    private DiningTable table;
    private MenuItem pizza;

    @BeforeEach
    void setup() {
        restaurant = Restaurant.builder().id(1L).name("Casa").location("Maadi").build();
        table = DiningTable.builder().id(5L).tableNumber(2).capacity(4).restaurant(restaurant).build();
        MenuCategory cat = MenuCategory.builder().id(3L).name("Pizza & Pasta").build();
        pizza = MenuItem.builder().id(6L).category(cat).name("Margherita")
                .price(new BigDecimal("180.00")).available(true).build();
    }

    @Test
    void createOrder_shouldSucceed_whenTableBelongsToRestaurant() {
        when(restaurantRepository.findById(1L)).thenReturn(Optional.of(restaurant));
        when(diningTableRepository.findById(5L)).thenReturn(Optional.of(table));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> {
            Order o = i.getArgument(0);
            o.setId(1L);
            return o;
        });

        OrderResponse res = orderService.createOrder(
                CreateOrderRequest.builder().tableId(5L).restaurantId(1L).notes("Window seat").build(), 7L);

        assertThat(res.getStatus()).isEqualTo("PENDING");
        assertThat(res.getUserId()).isEqualTo(7L);
    }

    @Test
    void createOrder_shouldThrow_whenTableBelongsToAnotherRestaurant() {
        Restaurant other = Restaurant.builder().id(2L).name("Other").location("Zayed").build();
        when(restaurantRepository.findById(2L)).thenReturn(Optional.of(other));
        when(diningTableRepository.findById(5L)).thenReturn(Optional.of(table));

        assertThatThrownBy(() -> orderService.createOrder(
                CreateOrderRequest.builder().tableId(5L).restaurantId(2L).build(), 7L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("does not belong");
    }

    @Test
    void addItem_shouldMergeQuantity_whenSameItemAddedTwice() {
        Order order = Order.builder().id(1L).tableId(5L).restaurantId(1L)
                .status(OrderStatus.PENDING).items(new HashSet<>()).build();
        OrderItem existing = OrderItem.builder().id(11L).order(order).menuItem(pizza)
                .quantity(1).unitPrice(pizza.getPrice()).build();
        order.getItems().add(existing);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(menuItemRepository.findById(6L)).thenReturn(Optional.of(pizza));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        OrderResponse res = orderService.addItem(1L,
                AddOrderItemRequest.builder().menuItemId(6L).quantity(2).build());

        assertThat(res.getItems()).hasSize(1);
        assertThat(res.getItems().get(0).getQuantity()).isEqualTo(3);
    }

    @Test
    void addItem_shouldThrow_whenOrderCompleted() {
        Order order = Order.builder().id(1L).status(OrderStatus.COMPLETED).items(new HashSet<>()).build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.addItem(1L,
                AddOrderItemRequest.builder().menuItemId(6L).quantity(1).build()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("completed");
    }

    @Test
    void addItem_shouldThrow_whenItemUnavailable() {
        Order order = Order.builder().id(1L).status(OrderStatus.PENDING).items(new HashSet<>()).build();
        pizza.setAvailable(false);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(menuItemRepository.findById(6L)).thenReturn(Optional.of(pizza));

        assertThatThrownBy(() -> orderService.addItem(1L,
                AddOrderItemRequest.builder().menuItemId(6L).quantity(1).build()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("unavailable");
    }

    @Test
    void updateStatus_shouldThrow_onIllegalTransition() {
        Order order = Order.builder().id(1L).status(OrderStatus.PENDING).items(new HashSet<>()).build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateStatus(1L,
                UpdateOrderStatusRequest.builder().status("COMPLETED").build()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot transition");
    }

    @Test
    void updateStatus_shouldSucceed_onLegalTransition() {
        Order order = Order.builder().id(1L).status(OrderStatus.PENDING).items(new HashSet<>()).build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        OrderResponse res = orderService.updateStatus(1L,
                UpdateOrderStatusRequest.builder().status("confirmed").build());

        assertThat(res.getStatus()).isEqualTo("CONFIRMED");
    }

    @Test
    void cancelOrder_shouldThrow_whenAlreadyCompleted() {
        Order order = Order.builder().id(1L).status(OrderStatus.COMPLETED).items(new HashSet<>()).build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(1L))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void removeItem_shouldThrow_whenItemNotInOrder() {
        Order order = Order.builder().id(1L).status(OrderStatus.PENDING).items(new HashSet<>()).build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.removeItem(1L, 999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
