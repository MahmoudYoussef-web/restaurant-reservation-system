package com.mahmoud.reservation.controller;

import com.mahmoud.reservation.dto.menu.MenuCategoryResponse;
import com.mahmoud.reservation.service.menu.MenuService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/restaurants/{id}/menu")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    @Operation(summary = "Get restaurant menu (public)")
    @GetMapping
    public ResponseEntity<List<MenuCategoryResponse>> getMenu(@PathVariable Long id) {
        return ResponseEntity.ok(menuService.getMenuByRestaurant(id));
    }
}
