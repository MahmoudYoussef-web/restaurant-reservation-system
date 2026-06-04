package com.mahmoud.reservation.dto.order;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddOrderItemRequest {

    @NotNull
    private Long menuItemId;

    @Min(1)
    private int quantity;
}
