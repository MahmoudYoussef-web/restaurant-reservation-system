package com.mahmoud.reservation.service;

import com.mahmoud.reservation.dto.common.PageResponse;
import com.mahmoud.reservation.dto.reservation.CreateReservationRequest;
import com.mahmoud.reservation.dto.reservation.ReservationResponse;
import com.mahmoud.reservation.dto.table.DiningTableResponse;
import com.mahmoud.reservation.entity.DiningTable;
import com.mahmoud.reservation.entity.Reservation;
import com.mahmoud.reservation.entity.Restaurant;
import com.mahmoud.reservation.enums.ReservationStatus;
import com.mahmoud.reservation.enums.TableStatus;
import com.mahmoud.reservation.exception.BadRequestException;
import com.mahmoud.reservation.exception.ForbiddenException;
import com.mahmoud.reservation.exception.ReservationConflictException;
import com.mahmoud.reservation.exception.ResourceNotFoundException;
import com.mahmoud.reservation.mapper.ReservationMapper;
import com.mahmoud.reservation.repository.DiningTableRepository;
import com.mahmoud.reservation.repository.ReservationRepository;
import com.mahmoud.reservation.service.reservation.ReservationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceImplTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private DiningTableRepository diningTableRepository;

    @Mock
    private ReservationMapper reservationMapper;

    @InjectMocks
    private ReservationServiceImpl reservationService;

    @Test
    void createReservation_shouldSucceed() {
        Long userId = 1L;
        Instant start = Instant.now().plus(1, ChronoUnit.HOURS);
        Instant end = start.plus(2, ChronoUnit.HOURS);

        CreateReservationRequest request = CreateReservationRequest.builder()
                .tableId(10L)
                .startTime(start)
                .endTime(end)
                .numberOfGuests(3)
                .specialRequest("Window seat")
                .build();

        Restaurant restaurant = Restaurant.builder().id(100L).build();

        DiningTable table = DiningTable.builder()
                .id(10L)
                .tableNumber(5)
                .capacity(4)
                .restaurant(restaurant)
                .build();

        when(diningTableRepository.findWithLockById(10L)).thenReturn(Optional.of(table));
        when(reservationRepository.existsConflict(anyLong(), any(), any(), anyList())).thenReturn(false);

        Reservation savedReservation = Reservation.builder()
                .id(1L)
                .userId(userId)
                .table(table)
                .startTime(start)
                .endTime(end)
                .status(ReservationStatus.PENDING)
                .numberOfGuests(3)
                .specialRequest("Window seat")
                .build();

        when(reservationRepository.save(any(Reservation.class))).thenReturn(savedReservation);

        ReservationResponse expectedResponse = ReservationResponse.builder()
                .id(1L)
                .tableId(10L)
                .restaurantId(100L)
                .startTime(start)
                .endTime(end)
                .status("PENDING")
                .numberOfGuests(3)
                .build();

        when(reservationMapper.toResponse(savedReservation)).thenReturn(expectedResponse);

        ReservationResponse response = reservationService.createReservation(request, userId);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getStatus()).isEqualTo("PENDING");
        verify(reservationRepository).save(any(Reservation.class));
    }

    @Test
    void createReservation_shouldThrow_whenInvalidTimeRange() {
        CreateReservationRequest request = CreateReservationRequest.builder()
                .tableId(1L)
                .startTime(Instant.now().plus(2, ChronoUnit.HOURS))
                .endTime(Instant.now().plus(1, ChronoUnit.HOURS))
                .numberOfGuests(2)
                .build();

        assertThatThrownBy(() -> reservationService.createReservation(request, 1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid time range");

        verify(diningTableRepository, never()).findWithLockById(any());
    }

    @Test
    void createReservation_shouldThrow_whenTableNotFound() {
        CreateReservationRequest request = CreateReservationRequest.builder()
                .tableId(999L)
                .startTime(Instant.now().plus(1, ChronoUnit.HOURS))
                .endTime(Instant.now().plus(2, ChronoUnit.HOURS))
                .numberOfGuests(2)
                .build();

        when(diningTableRepository.findWithLockById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservationService.createReservation(request, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createReservation_shouldThrow_whenCapacityExceeded() {
        CreateReservationRequest request = CreateReservationRequest.builder()
                .tableId(1L)
                .startTime(Instant.now().plus(1, ChronoUnit.HOURS))
                .endTime(Instant.now().plus(2, ChronoUnit.HOURS))
                .numberOfGuests(10)
                .build();

        DiningTable table = DiningTable.builder().id(1L).capacity(4).build();
        when(diningTableRepository.findWithLockById(1L)).thenReturn(Optional.of(table));

        assertThatThrownBy(() -> reservationService.createReservation(request, 1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Capacity exceeded");

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void createReservation_shouldThrow_whenTimeSlotConflict() {
        CreateReservationRequest request = CreateReservationRequest.builder()
                .tableId(1L)
                .startTime(Instant.now().plus(1, ChronoUnit.HOURS))
                .endTime(Instant.now().plus(2, ChronoUnit.HOURS))
                .numberOfGuests(2)
                .build();

        DiningTable table = DiningTable.builder().id(1L).capacity(4).build();
        when(diningTableRepository.findWithLockById(1L)).thenReturn(Optional.of(table));
        when(reservationRepository.existsConflict(anyLong(), any(), any(), anyList())).thenReturn(true);

        assertThatThrownBy(() -> reservationService.createReservation(request, 1L))
                .isInstanceOf(ReservationConflictException.class)
                .hasMessageContaining("Time slot already booked");
    }

    @Test
    void cancelReservation_shouldSucceed() {
        Long userId = 1L;
        Reservation reservation = Reservation.builder()
                .id(1L)
                .userId(userId)
                .status(ReservationStatus.APPROVED)
                .build();

        when(reservationRepository.findByIdWithLock(1L)).thenReturn(Optional.of(reservation));

        reservationService.cancelReservation(1L, userId);

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
    }

    @Test
    void cancelReservation_shouldThrow_whenNotOwner() {
        Reservation reservation = Reservation.builder()
                .id(1L)
                .userId(1L)
                .status(ReservationStatus.APPROVED)
                .build();

        when(reservationRepository.findByIdWithLock(1L)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> reservationService.cancelReservation(1L, 2L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Access denied");
    }

    @Test
    void cancelReservation_shouldThrow_whenAlreadyCancelled() {
        Reservation reservation = Reservation.builder()
                .id(1L)
                .userId(1L)
                .status(ReservationStatus.CANCELLED)
                .build();

        when(reservationRepository.findByIdWithLock(1L)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> reservationService.cancelReservation(1L, 1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Already cancelled");
    }

    @Test
    void getReservationById_shouldReturnReservation() {
        Long userId = 1L;
        Reservation reservation = Reservation.builder()
                .id(1L)
                .userId(userId)
                .build();

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));

        ReservationResponse expected = ReservationResponse.builder().id(1L).build();
        when(reservationMapper.toResponse(reservation)).thenReturn(expected);

        ReservationResponse response = reservationService.getReservationById(1L, userId);

        assertThat(response.getId()).isEqualTo(1L);
    }

    @Test
    void getReservationById_shouldThrow_whenNotOwner() {
        Reservation reservation = Reservation.builder()
                .id(1L)
                .userId(1L)
                .build();

        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> reservationService.getReservationById(1L, 2L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void getAvailableTables_shouldReturnAvailableTables() {
        Long restaurantId = 100L;
        Instant start = Instant.now().plus(1, ChronoUnit.HOURS);
        Instant end = start.plus(2, ChronoUnit.HOURS);

        DiningTable table = DiningTable.builder()
                .id(1L)
                .tableNumber(5)
                .capacity(4)
                .build();

        when(diningTableRepository.findAvailableTables(anyLong(), any(), any(), anyList(), any(TableStatus.class)))
                .thenReturn(List.of(table));

        List<DiningTableResponse> available = reservationService.getAvailableTables(
                restaurantId, start, end
        );

        assertThat(available).hasSize(1);
        assertThat(available.get(0).getTableNumber()).isEqualTo(5);
    }

    @Test
    void getAvailableTables_shouldThrow_whenInvalidTimeRange() {
        assertThatThrownBy(() -> reservationService.getAvailableTables(
                1L,
                Instant.now().plus(2, ChronoUnit.HOURS),
                Instant.now().plus(1, ChronoUnit.HOURS)
        )).isInstanceOf(BadRequestException.class);
    }

    @Test
    void getUserReservations_shouldReturnPagedResults() {
        Long userId = 1L;

        Reservation reservation = Reservation.builder()
                .id(1L)
                .userId(userId)
                .build();

        Page<Reservation> page = new PageImpl<>(List.of(reservation));
        when(reservationRepository.findByUserIdWithDetails(anyLong(), any(PageRequest.class)))
                .thenReturn(page);

        ReservationResponse response = ReservationResponse.builder().id(1L).build();
        when(reservationMapper.toResponseList(anyList())).thenReturn(List.of(response));

        PageResponse<ReservationResponse> result = reservationService.getUserReservations(userId, 0, 10);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getPage()).isZero();
    }
}
