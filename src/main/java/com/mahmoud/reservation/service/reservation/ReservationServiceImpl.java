package com.mahmoud.reservation.service.reservation;

import com.mahmoud.reservation.dto.common.PageResponse;
import com.mahmoud.reservation.dto.reservation.CreateReservationRequest;
import com.mahmoud.reservation.dto.reservation.ReservationResponse;
import com.mahmoud.reservation.dto.table.DiningTableResponse;
import com.mahmoud.reservation.entity.DiningTable;
import com.mahmoud.reservation.entity.Reservation;
import com.mahmoud.reservation.enums.ReservationStatus;
import com.mahmoud.reservation.enums.TableStatus;
import com.mahmoud.reservation.exception.*;
import com.mahmoud.reservation.mapper.ReservationMapper;
import com.mahmoud.reservation.repository.DiningTableRepository;
import com.mahmoud.reservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ReservationServiceImpl implements ReservationService {

    private static final Logger log = LoggerFactory.getLogger(ReservationServiceImpl.class);

    private final ReservationRepository reservationRepository;
    private final DiningTableRepository diningTableRepository;
    private final ReservationMapper reservationMapper;

    private static final int MAX_PAGE_SIZE = 100;

    @Override
    public ReservationResponse createReservation(CreateReservationRequest request, Long userId) {

        if (!request.getStartTime().isBefore(request.getEndTime())) {
            throw new BadRequestException("Invalid time range");
        }

        if (!request.getStartTime().isAfter(Instant.now())) {
            throw new BadRequestException("Reservation must start in the future");
        }

        DiningTable table = diningTableRepository.findWithLockById(request.getTableId())
                .orElseThrow(() -> new ResourceNotFoundException("Table not found"));

        if (request.getNumberOfGuests() > table.getCapacity()) {
            throw new BadRequestException("Capacity exceeded");
        }

        List<ReservationStatus> statuses = List.of(
                ReservationStatus.PENDING,
                ReservationStatus.APPROVED
        );

        boolean conflict = reservationRepository.existsConflict(
                table.getId(),
                request.getStartTime(),
                request.getEndTime(),
                statuses
        );

        if (conflict) {
            throw new ReservationConflictException("Time slot already booked");
        }

        Reservation reservation = Reservation.builder()
                .userId(userId)
                .table(table)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .numberOfGuests(request.getNumberOfGuests())
                .specialRequest(request.getSpecialRequest())
                .status(ReservationStatus.PENDING)
                .build();

        return reservationMapper.toResponse(reservationRepository.save(reservation));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReservationResponse> getUserReservations(Long userId, int page, int size) {
        validatePagination(page, size);
        PageRequest pageable = PageRequest.of(page, size);
        Page<Reservation> result = reservationRepository.findByUserIdWithDetails(userId, pageable);

        return PageResponse.<ReservationResponse>builder()
                .content(reservationMapper.toResponseList(result.getContent()))
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .last(result.isLast())
                .build();
    }

    @Override
    public void cancelReservation(Long reservationId, Long userId) {

        Reservation reservation = reservationRepository.findByIdWithLock(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found"));

        if (!reservation.getUserId().equals(userId)) {
            throw new ForbiddenException("Access denied");
        }

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new BadRequestException("Already cancelled");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
    }

    @Override
    @Transactional(readOnly = true)
    public ReservationResponse getReservationById(Long reservationId, Long userId) {

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found"));

        if (!reservation.getUserId().equals(userId)) {
            throw new ForbiddenException("Access denied");
        }

        return reservationMapper.toResponse(reservation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DiningTableResponse> getAvailableTables(Long restaurantId, Instant startTime, Instant endTime) {

        if (startTime == null || endTime == null) {
            throw new BadRequestException("Start time and end time are required");
        }

        if (!startTime.isBefore(endTime)) {
            throw new BadRequestException("Invalid time range");
        }

        if (!startTime.isAfter(Instant.now())) {
            throw new BadRequestException("Start time must be in the future");
        }

        List<ReservationStatus> statuses = List.of(
                ReservationStatus.PENDING,
                ReservationStatus.APPROVED
        );

        return diningTableRepository.findAvailableTables(
                        restaurantId,
                        startTime,
                        endTime,
                        statuses,
                        TableStatus.AVAILABLE
                )
                .stream()
                .map(t -> DiningTableResponse.builder()
                        .id(t.getId())
                        .tableNumber(t.getTableNumber())
                        .capacity(t.getCapacity())
                        .restaurantId(restaurantId)
                        .build())
                .toList();
    }

    @Scheduled(fixedRate = 3_600_000)
    @Transactional
    public void completeExpiredReservations() {
        List<Reservation> expired = reservationRepository
                .findByStatusAndEndTimeBefore(ReservationStatus.APPROVED, Instant.now());

        if (!expired.isEmpty()) {
            expired.forEach(r -> r.setStatus(ReservationStatus.COMPLETED));
            log.info("Completed {} expired reservations", expired.size());
        }
    }

    private void validatePagination(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new BadRequestException("Invalid pagination parameters (page >= 0, 1 <= size <= 100)");
        }
    }
}