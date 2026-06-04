package com.mahmoud.reservation.dto.admin;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDiningTableRequest {

    @NotNull
    @Min(1)
    private Integer tableNumber;

    @NotNull
    @Min(1)
    private Integer capacity;
}
