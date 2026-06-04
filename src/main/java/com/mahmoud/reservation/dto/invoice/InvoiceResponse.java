package com.mahmoud.reservation.dto.invoice;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceResponse {

    private Long id;
    private Long orderId;
    private BigDecimal subtotal;
    private BigDecimal taxPercent;
    private BigDecimal serviceChargePercent;
    private BigDecimal taxAmount;
    private BigDecimal serviceChargeAmount;
    private BigDecimal total;
    private Instant generatedAt;
}
