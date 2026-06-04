package com.mahmoud.reservation.repository;

import com.mahmoud.reservation.entity.*;
import com.mahmoud.reservation.enums.ReservationStatus;
import com.mahmoud.reservation.enums.TableStatus;
import com.mahmoud.reservation.enums.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class DiningTableRepositoryTest {

    @Autowired
    private DiningTableRepository diningTableRepository;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private UserRepository userRepository;

    private Restaurant restaurant;
    private DiningTable table1;
    private DiningTable table2;
    private User user;

    @BeforeEach
    void setUp() {
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

        restaurant = restaurantRepository.save(
                Restaurant.builder()
                        .name("Test Restaurant")
                        .location("Location")
                        .build()
        );

        table1 = diningTableRepository.save(
                DiningTable.builder()
                        .tableNumber(1)
                        .capacity(4)
                        .restaurant(restaurant)
                        .build()
        );

        table2 = diningTableRepository.save(
                DiningTable.builder()
                        .tableNumber(2)
                        .capacity(6)
                        .restaurant(restaurant)
                        .build()
        );
    }

    @Test
    void findAvailableTables_shouldExcludeBookedTables() {
        Instant start = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        Instant end = start.plus(2, ChronoUnit.HOURS);

        reservationRepository.save(
                Reservation.builder()
                        .userId(user.getId())
                        .table(table1)
                        .startTime(start)
                        .endTime(end)
                        .status(ReservationStatus.APPROVED)
                        .numberOfGuests(2)
                        .build()
        );

        List<DiningTable> available = diningTableRepository.findAvailableTables(
                restaurant.getId(),
                start.plus(1, ChronoUnit.HOURS),
                end.plus(1, ChronoUnit.HOURS),
                List.of(ReservationStatus.PENDING, ReservationStatus.APPROVED),
                TableStatus.AVAILABLE
        );

        assertThat(available).hasSize(1);
        assertThat(available.get(0).getId()).isEqualTo(table2.getId());
    }

    @Test
    void findAvailableTables_shouldReturnAllTables_whenNoOverlap() {
        Instant start = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        Instant end = start.plus(1, ChronoUnit.HOURS);

        reservationRepository.save(
                Reservation.builder()
                        .userId(user.getId())
                        .table(table1)
                        .startTime(start)
                        .endTime(end)
                        .status(ReservationStatus.APPROVED)
                        .numberOfGuests(2)
                        .build()
        );

        List<DiningTable> available = diningTableRepository.findAvailableTables(
                restaurant.getId(),
                end.plus(1, ChronoUnit.HOURS),
                end.plus(2, ChronoUnit.HOURS),
                List.of(ReservationStatus.PENDING, ReservationStatus.APPROVED),
                TableStatus.AVAILABLE
        );

        assertThat(available).hasSize(2);
    }

    @Test
    void findWithLockById_shouldReturnTable() {
        Optional<DiningTable> found = diningTableRepository.findWithLockById(table1.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(table1.getId());
    }

    @Test
    void existsByRestaurantIdAndTableNumber_shouldReturnTrue_whenExists() {
        boolean exists = diningTableRepository.existsByRestaurantIdAndTableNumber(
                restaurant.getId(), 1
        );
        assertThat(exists).isTrue();
    }

    @Test
    void existsByRestaurantIdAndTableNumber_shouldReturnFalse_whenNotExists() {
        boolean exists = diningTableRepository.existsByRestaurantIdAndTableNumber(
                restaurant.getId(), 999
        );
        assertThat(exists).isFalse();
    }

    @Test
    void findByRestaurantId_shouldReturnPagedResults() {
        Page<DiningTable> page = diningTableRepository.findByRestaurantId(
                restaurant.getId(), PageRequest.of(0, 1)
        );
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getTotalElements()).isEqualTo(2);
    }
}
