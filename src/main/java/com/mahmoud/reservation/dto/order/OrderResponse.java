package com.mahmoud.reservation.dto.order;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

    private Long id;
    private Long tableId;
    private Long restaurantId;
    private Long userId;
    private String status;
    private String notes;
    private Instant createdAt;
    private List<OrderItemResponse> items;
    private BigDecimal total;
}
