package com.mahmoud.reservation.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.HashSet;
import java.util.Set;

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