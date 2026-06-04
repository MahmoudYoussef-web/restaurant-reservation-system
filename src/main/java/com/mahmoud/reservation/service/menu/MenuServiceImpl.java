package com.mahmoud.reservation.service.menu;

import com.mahmoud.reservation.dto.menu.MenuCategoryResponse;
import com.mahmoud.reservation.dto.menu.MenuItemResponse;
import com.mahmoud.reservation.entity.MenuCategory;
import com.mahmoud.reservation.repository.MenuCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MenuServiceImpl implements MenuService {

    private final MenuCategoryRepository menuCategoryRepository;

    @Override
    public List<MenuCategoryResponse> getMenuByRestaurant(Long restaurantId) {
        return menuCategoryRepository.findAll().stream()
                .map(this::toCategoryResponse)
                .toList();
    }

    private MenuCategoryResponse toCategoryResponse(MenuCategory category) {
        return MenuCategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .menuItems(category.getMenuItems().stream()
                        .filter(item -> item.isAvailable())
                        .map(item -> MenuItemResponse.builder()
                                .id(item.getId())
                                .categoryId(category.getId())
                                .name(item.getName())
                                .description(item.getDescription())
                                .price(item.getPrice())
                                .imageUrl(item.getImageUrl())
                                .available(item.isAvailable())
                                .build())
                        .toList())
                .build();
    }
}
