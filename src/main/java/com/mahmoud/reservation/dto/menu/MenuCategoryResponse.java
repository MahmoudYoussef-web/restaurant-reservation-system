package com.mahmoud.reservation.dto.menu;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuCategoryResponse {

    private Long id;
    private String name;
    private String description;
    private List<MenuItemResponse> menuItems;
}
