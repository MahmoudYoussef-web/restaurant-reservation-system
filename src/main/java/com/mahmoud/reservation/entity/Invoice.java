package com.mahmoud.reservation.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.SQLRestriction;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.Instant;

@SQLRestriction("is_deleted = false")
@Entity
@Table(name = "invoices")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Invoice extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @Column(name = "subtotal", nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "tax_percent", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal taxPercent = new BigDecimal("14.00");

    @Column(name = "service_charge_percent", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal serviceChargePercent = new BigDecimal("10.00");

    @Column(name = "tax_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal taxAmount;

    @Column(name = "service_charge_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal serviceChargeAmount;

    @Column(name = "total", nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;
}
