package com.mahmoud.reservation.service;

import com.mahmoud.reservation.dto.common.PageResponse;
import com.mahmoud.reservation.dto.restaurant.RestaurantResponse;
import com.mahmoud.reservation.entity.Restaurant;
import com.mahmoud.reservation.exception.BadRequestException;
import com.mahmoud.reservation.exception.ResourceNotFoundException;
import com.mahmoud.reservation.repository.RestaurantRepository;
import com.mahmoud.reservation.service.restaurant.RestaurantServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RestaurantServiceImplTest {

    @Mock
    private RestaurantRepository restaurantRepository;

    @InjectMocks
    private RestaurantServiceImpl restaurantService;

    @Test
    void browseRestaurants_shouldSearchByNameOrLocation() {
        Restaurant r = Restaurant.builder().id(1L).name("Casa Italia").location("Maadi").build();
        when(restaurantRepository.findByNameContainingIgnoreCaseOrLocationContainingIgnoreCaseOrCuisineContainingIgnoreCase(
                eq("maadi"), eq("maadi"), eq("maadi"), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(r)));

        PageResponse<RestaurantResponse> res = restaurantService.browseRestaurants("maadi", 0, 10);

        assertThat(res.getContent()).hasSize(1);
        assertThat(res.getContent().get(0).getName()).isEqualTo("Casa Italia");
    }

    @Test
    void browseRestaurants_shouldMatchCuisine() {
        Restaurant r = Restaurant.builder().id(1L).name("Casa Italia").location("Maadi").cuisine("Italian").build();
        when(restaurantRepository.findByNameContainingIgnoreCaseOrLocationContainingIgnoreCaseOrCuisineContainingIgnoreCase(
                eq("italian"), eq("italian"), eq("italian"), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(r)));

        PageResponse<RestaurantResponse> res = restaurantService.browseRestaurants("italian", 0, 10);

        assertThat(res.getContent()).hasSize(1);
        assertThat(res.getContent().get(0).getCuisine()).isEqualTo("Italian");
    }

    @Test
    void browseRestaurants_shouldRejectOversizedPage() {
        assertThatThrownBy(() -> restaurantService.browseRestaurants(null, 0, 101))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void getRestaurantById_shouldThrow_whenMissing() {
        when(restaurantRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> restaurantService.getRestaurantById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
