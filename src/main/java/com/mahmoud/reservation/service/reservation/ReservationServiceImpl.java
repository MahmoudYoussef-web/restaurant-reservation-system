package com.mahmoud.reservation.service.reservation;

import com.mahmoud.reservation.dto.reservation.CreateReservationRequest;
import com.mahmoud.reservation.dto.reservation.ReservationResponse;
import com.mahmoud.reservation.dto.table.DiningTableResponse;
import com.mahmoud.reservation.entity.DiningTable;
import com.mahmoud.reservation.entity.Reservation;
import com.mahmoud.reservation.enums.ReservationStatus;
import com.mahmoud.reservation.exception.BadRequestException;
import com.mahmoud.reservation.exception.ForbiddenException;
import com.mahmoud.reservation.exception.ReservationConflictException;
import com.mahmoud.reservation.exception.ResourceNotFoundException;
import com.mahmoud.reservation.mapper.ReservationMapper;
import com.mahmoud.reservation.repository.DiningTableRepository;
import com.mahmoud.reservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ReservationServiceImpl implements ReservationService {

    private final ReservationRepository reservationRepository;
    private final DiningTableRepository diningTableRepository;

    @Override
    public ReservationResponse createReservation(CreateReservationRequest request, Long userId) {

        if (request.getStartTime() == null || request.getEndTime() == null ||
                !request.getStartTime().isBefore(request.getEndTime())) {
            throw new BadRequestException("Invalid reservation time range");
        }

        DiningTable table = diningTableRepository.findWithLockById(request.getTableId())
                .orElseThrow(() -> new ResourceNotFoundException("Dining table not found"));

        if (request.getNumberOfGuests() > table.getCapacity()) {
            throw new BadRequestException("Number of guests exceeds table capacity");
        }

        List<ReservationStatus> activeStatuses = List.of(
                ReservationStatus.PENDING,
                ReservationStatus.CONFIRMED
        );

        List<Reservation> conflicts = reservationRepository.findConflictingReservations(
                table.getId(),
                request.getStartTime(),
                request.getEndTime(),
                activeStatuses
        );

        if (!conflicts.isEmpty()) {
            throw new ReservationConflictException("Time slot is already booked");
        }

        Reservation reservation = Reservation.builder()
                .userId(userId)
                .table(table)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .numberOfGuests(request.getNumberOfGuests())
                .specialRequest(request.getSpecialRequest())
                .status(ReservationStatus.CONFIRMED)
                .build();

        return ReservationMapper.toResponse(reservationRepository.save(reservation));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservationResponse> getUserReservations(Long userId) {
        return ReservationMapper.toResponseList(
                reservationRepository.findByUserId(userId)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ReservationResponse getReservationById(Long reservationId, Long userId) {

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found"));

        if (!reservation.getUserId().equals(userId)) {
            throw new ForbiddenException("Access denied");
        }

        return ReservationMapper.toResponse(reservation);
    }

    @Override
    public void cancelReservation(Long reservationId, Long userId) {

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found"));

        if (!reservation.getUserId().equals(userId)) {
            throw new ForbiddenException("Access denied");
        }

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new BadRequestException("Reservation already cancelled");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservationRepository.save(reservation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DiningTableResponse> getAvailableTables(Long restaurantId, Instant startTime, Instant endTime) {

        if (startTime == null || endTime == null || !startTime.isBefore(endTime)) {
            throw new BadRequestException("Invalid time range");
        }

        List<DiningTable> tables = diningTableRepository.findByRestaurantId(restaurantId);

        if (tables.isEmpty()) {
            return List.of();
        }

        List<ReservationStatus> statuses = List.of(
                ReservationStatus.PENDING,
                ReservationStatus.CONFIRMED
        );

        List<Long> conflictingTableIds = reservationRepository.findConflictingTableIds(
                restaurantId,
                startTime,
                endTime,
                statuses
        );

        HashSet<Long> conflictingSet = new HashSet<>(conflictingTableIds);

        return tables.stream()
                .filter(t -> !conflictingSet.contains(t.getId()))
                .map(t -> DiningTableResponse.builder()
                        .id(t.getId())
                        .tableNumber(t.getTableNumber())
                        .capacity(t.getCapacity())
                        .restaurantId(restaurantId)
                        .build())
                .toList();
    }
}