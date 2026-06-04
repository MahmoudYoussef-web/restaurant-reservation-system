package com.mahmoud.reservation.service;

import com.mahmoud.reservation.dto.admin.CreateDiningTableRequest;
import com.mahmoud.reservation.dto.admin.CreateRestaurantRequest;
import com.mahmoud.reservation.dto.common.PageResponse;
import com.mahmoud.reservation.dto.restaurant.RestaurantResponse;
import com.mahmoud.reservation.dto.table.DiningTableResponse;
import com.mahmoud.reservation.entity.DiningTable;
import com.mahmoud.reservation.entity.Restaurant;
import com.mahmoud.reservation.exception.BadRequestException;
import com.mahmoud.reservation.exception.ConflictException;
import com.mahmoud.reservation.exception.ResourceNotFoundException;
import com.mahmoud.reservation.repository.DiningTableRepository;
import com.mahmoud.reservation.repository.RestaurantRepository;
import com.mahmoud.reservation.service.admin.AdminServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceImplTest {

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private DiningTableRepository diningTableRepository;

    @InjectMocks
    private AdminServiceImpl adminService;

    @Test
    void createRestaurant_shouldSucceed() {
        CreateRestaurantRequest request = CreateRestaurantRequest.builder()
                .name("New Restaurant")
                .location("Downtown")
                .build();

        when(restaurantRepository.existsByName("New Restaurant")).thenReturn(false);

        Restaurant savedRestaurant = Restaurant.builder()
                .id(1L)
                .name("New Restaurant")
                .location("Downtown")
                .build();

        when(restaurantRepository.save(any(Restaurant.class))).thenReturn(savedRestaurant);

        RestaurantResponse response = adminService.createRestaurant(request);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("New Restaurant");
    }

    @Test
    void createRestaurant_shouldThrow_whenNameExists() {
        CreateRestaurantRequest request = CreateRestaurantRequest.builder()
                .name("Duplicate")
                .build();

        when(restaurantRepository.existsByName("Duplicate")).thenReturn(true);

        assertThatThrownBy(() -> adminService.createRestaurant(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already exists");

        verify(restaurantRepository, never()).save(any());
    }

    @Test
    void createDiningTable_shouldSucceed() {
        CreateDiningTableRequest request = CreateDiningTableRequest.builder()
                .restaurantId(1L)
                .tableNumber(5)
                .capacity(4)
                .build();

        Restaurant restaurant = Restaurant.builder().id(1L).name("Test").build();

        when(restaurantRepository.findById(1L)).thenReturn(Optional.of(restaurant));
        when(diningTableRepository.existsByRestaurantIdAndTableNumber(1L, 5)).thenReturn(false);

        DiningTable savedTable = DiningTable.builder()
                .id(10L)
                .tableNumber(5)
                .capacity(4)
                .restaurant(restaurant)
                .build();

        when(diningTableRepository.save(any(DiningTable.class))).thenReturn(savedTable);

        DiningTableResponse response = adminService.createDiningTable(request);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getTableNumber()).isEqualTo(5);
    }

    @Test
    void createDiningTable_shouldThrow_whenRestaurantNotFound() {
        CreateDiningTableRequest request = CreateDiningTableRequest.builder()
                .restaurantId(999L)
                .build();

        when(restaurantRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.createDiningTable(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createDiningTable_shouldThrow_whenTableNumberExists() {
        CreateDiningTableRequest request = CreateDiningTableRequest.builder()
                .restaurantId(1L)
                .tableNumber(5)
                .capacity(4)
                .build();

        Restaurant restaurant = Restaurant.builder().id(1L).build();
        when(restaurantRepository.findById(1L)).thenReturn(Optional.of(restaurant));
        when(diningTableRepository.existsByRestaurantIdAndTableNumber(1L, 5)).thenReturn(true);

        assertThatThrownBy(() -> adminService.createDiningTable(request))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void getAllRestaurants_shouldReturnPagedResults() {
        Restaurant restaurant = Restaurant.builder()
                .id(1L).name("Test").location("Loc").build();

        Page<Restaurant> page = new PageImpl<>(List.of(restaurant));
        when(restaurantRepository.findAll(any(PageRequest.class))).thenReturn(page);

        PageResponse<RestaurantResponse> response = adminService.getAllRestaurants(0, 10);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getName()).isEqualTo("Test");
    }

    @Test
    void getAllRestaurants_shouldValidatePagination() {
        assertThatThrownBy(() -> adminService.getAllRestaurants(-1, 10))
                .isInstanceOf(BadRequestException.class);

        assertThatThrownBy(() -> adminService.getAllRestaurants(0, 0))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void getTablesByRestaurant_shouldReturnPagedResults() {
        Long restaurantId = 1L;
        Restaurant restaurant = Restaurant.builder().id(restaurantId).build();

        DiningTable table = DiningTable.builder()
                .id(10L).tableNumber(1).capacity(4).restaurant(restaurant)
                .build();

        Page<DiningTable> page = new PageImpl<>(List.of(table));

        when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));
        when(diningTableRepository.findByRestaurantId(anyLong(), any(PageRequest.class)))
                .thenReturn(page);

        PageResponse<DiningTableResponse> response = adminService.getTablesByRestaurant(
                restaurantId, 0, 10
        );

        assertThat(response.getContent()).hasSize(1);
    }

    @Test
    void getTablesByRestaurant_shouldThrow_whenRestaurantNotFound() {
        when(restaurantRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.getTablesByRestaurant(999L, 0, 10))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
