package com.mahmoud.reservation.dto.admin;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRestaurantRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String location;
}