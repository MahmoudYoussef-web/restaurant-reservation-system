package com.mahmoud.reservation.dto.order;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {

    @NotNull
    private Long tableId;

    @NotNull
    private Long restaurantId;

    private String notes;
}
