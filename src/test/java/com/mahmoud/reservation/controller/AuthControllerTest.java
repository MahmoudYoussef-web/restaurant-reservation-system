package com.mahmoud.reservation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mahmoud.reservation.dto.auth.AuthResponse;
import com.mahmoud.reservation.dto.auth.LoginRequest;
import com.mahmoud.reservation.dto.auth.RefreshTokenRequest;
import com.mahmoud.reservation.dto.auth.RegisterRequest;
import com.mahmoud.reservation.security.jwt.AuthTokenFilter;
import com.mahmoud.reservation.security.jwt.JwtUtils;
import com.mahmoud.reservation.security.user.ShopUserDetailsService;
import com.mahmoud.reservation.service.auth.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtUtils jwtUtils;

    @MockBean
    private ShopUserDetailsService userDetailsService;

    @MockBean
    private AuthTokenFilter authTokenFilter;

    @Test
    void register_shouldReturn201() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .password("password123")
                .build();

        AuthResponse response = AuthResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .refreshTokenId("token-id")
                .build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshTokenId").value("token-id"));
    }

    @Test
    void login_shouldReturn200() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("john@example.com")
                .password("password123")
                .build();

        AuthResponse response = AuthResponse.builder()
                .accessToken("access-token")
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"));
    }

    @Test
    void refresh_shouldReturn200() throws Exception {
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("refresh-token")
                .refreshTokenId("token-id")
                .build();

        AuthResponse response = AuthResponse.builder()
                .accessToken("new-access-token")
                .build();

        when(authService.refreshToken("refresh-token", "token-id")).thenReturn(response);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"));
    }

    @Test
    void logout_shouldReturn200() throws Exception {
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("some-token")
                .refreshTokenId("token-id")
                .build();

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));
    }

    @Test
    void register_shouldReturn400_whenInvalidInput() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"\"}"))
                .andExpect(status().isBadRequest());
    }
}
