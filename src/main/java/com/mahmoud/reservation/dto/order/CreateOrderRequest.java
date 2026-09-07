package com.mahmoud.reservation.dto.order;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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

    @Size(max = 500)
    private String notes;
}
