package com.mahmoud.reservation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mahmoud.reservation.dto.common.PageResponse;
import com.mahmoud.reservation.dto.reservation.CreateReservationRequest;
import com.mahmoud.reservation.dto.reservation.ReservationResponse;
import com.mahmoud.reservation.security.jwt.AuthTokenFilter;
import com.mahmoud.reservation.security.jwt.JwtUtils;
import com.mahmoud.reservation.security.user.ShopUserDetails;
import com.mahmoud.reservation.security.user.ShopUserDetailsService;
import com.mahmoud.reservation.service.reservation.ReservationService;
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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReservationController.class)
@AutoConfigureMockMvc(addFilters = false)
class ReservationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReservationService reservationService;

    @MockBean
    private JwtUtils jwtUtils;

    @MockBean
    private ShopUserDetailsService userDetailsService;

    @MockBean
    private AuthTokenFilter authTokenFilter;

    @BeforeEach
    void setUp() {
        ShopUserDetails userDetails = new ShopUserDetails(
                1L, "test@example.com", "hash", true, true, List.of()
        );
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void createReservation_shouldReturn201() throws Exception {
        CreateReservationRequest request = CreateReservationRequest.builder()
                .tableId(1L)
                .startTime(Instant.now().plus(1, ChronoUnit.HOURS))
                .endTime(Instant.now().plus(2, ChronoUnit.HOURS))
                .numberOfGuests(2)
                .build();

        ReservationResponse response = ReservationResponse.builder()
                .id(1L)
                .status("PENDING")
                .build();

        when(reservationService.createReservation(any(CreateReservationRequest.class), anyLong()))
                .thenReturn(response);

        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getMyReservations_shouldReturn200() throws Exception {
        PageResponse<ReservationResponse> page = PageResponse.<ReservationResponse>builder()
                .content(List.of(ReservationResponse.builder().id(1L).build()))
                .page(0).size(10).totalElements(1).totalPages(1).last(true)
                .build();
        when(reservationService.getUserReservations(anyLong(), anyInt(), anyInt()))
                .thenReturn(page);

        mockMvc.perform(get("/api/reservations/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1));
    }

    @Test
    void cancelReservation_shouldReturn200() throws Exception {
        mockMvc.perform(delete("/api/reservations/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Reservation cancelled successfully"));
    }
}
