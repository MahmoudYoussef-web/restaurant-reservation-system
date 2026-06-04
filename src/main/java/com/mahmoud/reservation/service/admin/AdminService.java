package com.mahmoud.reservation.service.admin;

import com.mahmoud.reservation.dto.admin.*;
import com.mahmoud.reservation.dto.common.MessageResponse;
import com.mahmoud.reservation.dto.common.PageResponse;
import com.mahmoud.reservation.dto.menu.MenuCategoryRequest;
import com.mahmoud.reservation.dto.menu.MenuCategoryResponse;
import com.mahmoud.reservation.dto.menu.MenuItemRequest;
import com.mahmoud.reservation.dto.menu.MenuItemResponse;
import com.mahmoud.reservation.dto.restaurant.RestaurantResponse;
import com.mahmoud.reservation.dto.table.DiningTableResponse;

public interface AdminService {

    RestaurantResponse createRestaurant(CreateRestaurantRequest request);

    RestaurantResponse updateRestaurant(Long id, CreateRestaurantRequest request);

    void deleteRestaurant(Long id);

    DiningTableResponse createDiningTable(CreateDiningTableRequest request);

    DiningTableResponse updateDiningTable(Long id, UpdateDiningTableRequest request);

    void deleteDiningTable(Long id);

    DiningTableResponse updateTableStatus(Long id, UpdateTableStatusRequest request);

    PageResponse<RestaurantResponse> getAllRestaurants(int page, int size);

    PageResponse<DiningTableResponse> getTablesByRestaurant(Long restaurantId, int page, int size);

    MessageResponse assignOwnerRole(Long userId);

    MessageResponse cancelReservationByAdmin(Long reservationId);

    MessageResponse approveReservation(Long reservationId);

    MessageResponse rejectReservation(Long reservationId);

    MenuCategoryResponse createCategory(MenuCategoryRequest request);

    MenuCategoryResponse updateCategory(Long id, MenuCategoryRequest request);

    void deleteCategory(Long id);

    MenuItemResponse createMenuItem(MenuItemRequest request);

    MenuItemResponse updateMenuItem(Long id, MenuItemRequest request);

    void deleteMenuItem(Long id);
}