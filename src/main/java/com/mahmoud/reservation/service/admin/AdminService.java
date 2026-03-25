package com.mahmoud.reservation.service.admin;

import com.mahmoud.reservation.dto.admin.CreateDiningTableRequest;
import com.mahmoud.reservation.dto.admin.CreateRestaurantRequest;
import com.mahmoud.reservation.dto.common.PageResponse;
import com.mahmoud.reservation.dto.restaurant.RestaurantResponse;
import com.mahmoud.reservation.dto.table.DiningTableResponse;

public interface AdminService {

    RestaurantResponse createRestaurant(CreateRestaurantRequest request);

    DiningTableResponse createDiningTable(CreateDiningTableRequest request);

    PageResponse<RestaurantResponse> getAllRestaurants(int page, int size);

    PageResponse<DiningTableResponse> getTablesByRestaurant(Long restaurantId, int page, int size);
}