package com.mahmoud.reservation.entity;

import com.mahmoud.reservation.enums.TableStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.SQLRestriction;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.HashSet;
import java.util.Set;

@SQLRestriction("is_deleted = false")
@Entity
@Table(name = "dining_tables",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_restaurant_table_number",
                        columnNames = {"restaurant_id", "table_number"})
        },
        indexes = {
                @Index(name = "idx_table_restaurant", columnList = "restaurant_id"),
                @Index(name = "idx_table_capacity", columnList = "capacity")
        }
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class DiningTable extends BaseEntity {

    @Column(name = "table_number", nullable = false)
    private Integer tableNumber;

    @Column(name = "capacity", nullable = false)
    private Integer capacity;

    @Enumerated(EnumType.STRING)
    @Column(name = "table_status", nullable = false)
    @Builder.Default
    private TableStatus tableStatus = TableStatus.AVAILABLE;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @OneToMany(mappedBy = "table",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Reservation> reservations = new HashSet<>();
}