package com.mahmoud.reservation.repository;

import com.mahmoud.reservation.entity.*;
import com.mahmoud.reservation.enums.ReservationStatus;
import com.mahmoud.reservation.enums.RoleName;
import com.mahmoud.reservation.enums.TableStatus;
import com.mahmoud.reservation.enums.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ReservationRepositoryTest {

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private DiningTableRepository diningTableRepository;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    private User user;
    private DiningTable table;
    private Instant baseTime;

    @BeforeEach
    void setUp() {
        baseTime = Instant.now().truncatedTo(ChronoUnit.MILLIS);

        Role role = roleRepository.save(
                Role.builder().name(RoleName.ROLE_USER).build()
        );

        user = userRepository.save(
                User.builder()
                        .firstName("Test")
                        .lastName("User")
                        .email("test@example.com")
                        .passwordHash("hash")
                        .status(UserStatus.ACTIVE)
                        .emailVerified(true)
                        .build()
        );

        Restaurant restaurant = restaurantRepository.save(
                Restaurant.builder()
                        .name("Test Restaurant")
                        .location("Test Location")
                        .build()
        );

        table = diningTableRepository.save(
                DiningTable.builder()
                        .tableNumber(1)
                        .capacity(4)
                        .restaurant(restaurant)
                        .build()
        );
    }

    @Test
    void existsConflict_shouldReturnTrue_whenTimesOverlap() {
        Reservation reservation = reservationRepository.save(
                Reservation.builder()
                        .userId(user.getId())
                        .table(table)
                        .startTime(baseTime)
                        .endTime(baseTime.plus(2, ChronoUnit.HOURS))
                        .status(ReservationStatus.APPROVED)
                        .numberOfGuests(2)
                        .build()
        );

        boolean conflict = reservationRepository.existsConflict(
                table.getId(),
                baseTime.plus(1, ChronoUnit.HOURS),
                baseTime.plus(3, ChronoUnit.HOURS),
                List.of(ReservationStatus.PENDING, ReservationStatus.APPROVED)
        );

        assertThat(conflict).isTrue();
    }

    @Test
    void existsConflict_shouldReturnFalse_whenNoOverlap() {
        reservationRepository.save(
                Reservation.builder()
                        .userId(user.getId())
                        .table(table)
                        .startTime(baseTime)
                        .endTime(baseTime.plus(1, ChronoUnit.HOURS))
                        .status(ReservationStatus.APPROVED)
                        .numberOfGuests(2)
                        .build()
        );

        boolean conflict = reservationRepository.existsConflict(
                table.getId(),
                baseTime.plus(2, ChronoUnit.HOURS),
                baseTime.plus(3, ChronoUnit.HOURS),
                List.of(ReservationStatus.PENDING, ReservationStatus.APPROVED)
        );

        assertThat(conflict).isFalse();
    }

    @Test
    void existsConflict_shouldIgnoreCancelledReservations() {
        reservationRepository.save(
                Reservation.builder()
                        .userId(user.getId())
                        .table(table)
                        .startTime(baseTime)
                        .endTime(baseTime.plus(2, ChronoUnit.HOURS))
                        .status(ReservationStatus.CANCELLED)
                        .numberOfGuests(2)
                        .build()
        );

        boolean conflict = reservationRepository.existsConflict(
                table.getId(),
                baseTime.plus(1, ChronoUnit.HOURS),
                baseTime.plus(2, ChronoUnit.HOURS),
                List.of(ReservationStatus.PENDING, ReservationStatus.APPROVED)
        );

        assertThat(conflict).isFalse();
    }

    @Test
    void findByIdWithLock_shouldReturnReservation() {
        Reservation saved = reservationRepository.save(
                Reservation.builder()
                        .userId(user.getId())
                        .table(table)
                        .startTime(baseTime)
                        .endTime(baseTime.plus(1, ChronoUnit.HOURS))
                        .status(ReservationStatus.APPROVED)
                        .numberOfGuests(2)
                        .build()
        );

        Optional<Reservation> found = reservationRepository.findByIdWithLock(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
    }

    @Test
    void findByUserIdWithDetails_shouldReturnUserReservations() {
        reservationRepository.save(
                Reservation.builder()
                        .userId(user.getId())
                        .table(table)
                        .startTime(baseTime)
                        .endTime(baseTime.plus(1, ChronoUnit.HOURS))
                        .status(ReservationStatus.APPROVED)
                        .numberOfGuests(2)
                        .build()
        );

        List<Reservation> reservations = reservationRepository.findByUserIdWithDetails(user.getId());

        assertThat(reservations).hasSize(1);
        assertThat(reservations.get(0).getTable()).isNotNull();
        assertThat(reservations.get(0).getTable().getRestaurant()).isNotNull();
    }

    @Test
    void findConflictingTableIds_shouldReturnConflictingTable() {
        reservationRepository.save(
                Reservation.builder()
                        .userId(user.getId())
                        .table(table)
                        .startTime(baseTime)
                        .endTime(baseTime.plus(2, ChronoUnit.HOURS))
                        .status(ReservationStatus.APPROVED)
                        .numberOfGuests(2)
                        .build()
        );

        List<Long> conflictingIds = reservationRepository.findConflictingTableIds(
                table.getRestaurant().getId(),
                baseTime.plus(1, ChronoUnit.HOURS),
                baseTime.plus(3, ChronoUnit.HOURS),
                List.of(ReservationStatus.PENDING, ReservationStatus.APPROVED)
        );

        assertThat(conflictingIds).containsExactly(table.getId());
    }

    @Test
    void findConflictingTableIds_shouldReturnEmpty_whenNoConflict() {
        reservationRepository.save(
                Reservation.builder()
                        .userId(user.getId())
                        .table(table)
                        .startTime(baseTime)
                        .endTime(baseTime.plus(1, ChronoUnit.HOURS))
                        .status(ReservationStatus.APPROVED)
                        .numberOfGuests(2)
                        .build()
        );

        List<Long> conflictingIds = reservationRepository.findConflictingTableIds(
                table.getRestaurant().getId(),
                baseTime.plus(2, ChronoUnit.HOURS),
                baseTime.plus(3, ChronoUnit.HOURS),
                List.of(ReservationStatus.PENDING, ReservationStatus.APPROVED)
        );

        assertThat(conflictingIds).isEmpty();
    }
}
