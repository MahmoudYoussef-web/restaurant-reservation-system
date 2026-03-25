package com.mahmoud.reservation.entity;

import com.mahmoud.reservation.entity.BaseEntity;
import com.mahmoud.reservation.entity.DiningTable;
import com.mahmoud.reservation.enums.ReservationStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Entity
@Table(name = "reservations",
        indexes = {
                @Index(name = "idx_res_table_time",
                        columnList = "table_id, start_time, end_time"),
                @Index(name = "idx_res_user", columnList = "user_id")
        }
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Reservation extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "table_id", nullable = false)
    private DiningTable table;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReservationStatus status;

    @Column(name = "number_of_guests", nullable = false)
    private Integer numberOfGuests;

    @Column(name = "special_request")
    private String specialRequest;

    @PrePersist
    @PreUpdate
    private void validate() {
        if (endTime.isBefore(startTime)) {
            throw new IllegalArgumentException("Invalid reservation time range");
        }

        if (numberOfGuests == null || numberOfGuests <= 0) {
            throw new IllegalArgumentException("Invalid number of guests");
        }
    }
}