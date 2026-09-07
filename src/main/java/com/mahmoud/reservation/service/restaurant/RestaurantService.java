package com.mahmoud.reservation.service.restaurant;

import com.mahmoud.reservation.dto.common.PageResponse;
import com.mahmoud.reservation.dto.restaurant.RestaurantResponse;

public interface RestaurantService {

    PageResponse<RestaurantResponse> browseRestaurants(String search, int page, int size);

    RestaurantResponse getRestaurantById(Long id);
}
