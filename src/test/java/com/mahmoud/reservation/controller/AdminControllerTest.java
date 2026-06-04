package com.mahmoud.reservation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mahmoud.reservation.dto.admin.CreateRestaurantRequest;
import com.mahmoud.reservation.dto.common.PageResponse;
import com.mahmoud.reservation.dto.restaurant.RestaurantResponse;
import com.mahmoud.reservation.security.jwt.AuthTokenFilter;
import com.mahmoud.reservation.security.jwt.JwtUtils;
import com.mahmoud.reservation.security.user.ShopUserDetails;
import com.mahmoud.reservation.security.user.ShopUserDetailsService;
import com.mahmoud.reservation.service.admin.AdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AdminService adminService;

    @MockBean
    private JwtUtils jwtUtils;

    @MockBean
    private ShopUserDetailsService userDetailsService;

    @MockBean
    private AuthTokenFilter authTokenFilter;

    @BeforeEach
    void setUp() {
        ShopUserDetails userDetails = new ShopUserDetails(
                1L, "admin@example.com", "hash", true, true, List.of()
        );
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void createRestaurant_shouldReturn201() throws Exception {
        CreateRestaurantRequest request = CreateRestaurantRequest.builder()
                .name("Test Restaurant")
                .location("Downtown")
                .build();

        RestaurantResponse response = RestaurantResponse.builder()
                .id(1L)
                .name("Test Restaurant")
                .location("Downtown")
                .build();

        when(adminService.createRestaurant(any(CreateRestaurantRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/admin/restaurants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Test Restaurant"));
    }

    @Test
    void getAllRestaurants_shouldReturn200() throws Exception {
        PageResponse<RestaurantResponse> page = PageResponse.<RestaurantResponse>builder()
                .content(List.of(RestaurantResponse.builder().id(1L).name("Test").build()))
                .page(0)
                .size(10)
                .totalElements(1)
                .totalPages(1)
                .last(true)
                .build();

        when(adminService.getAllRestaurants(anyInt(), anyInt())).thenReturn(page);

        mockMvc.perform(get("/api/admin/restaurants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Test"));
    }
}
