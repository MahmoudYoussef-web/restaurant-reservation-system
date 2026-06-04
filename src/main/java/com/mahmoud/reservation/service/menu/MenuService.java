package com.mahmoud.reservation.service.menu;

import com.mahmoud.reservation.dto.menu.MenuCategoryResponse;

import java.util.List;

public interface MenuService {
    List<MenuCategoryResponse> getMenuByRestaurant(Long restaurantId);
}
