package com.mahmoud.reservation.dto.menu;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuCategoryRequest {

    @NotBlank
    private String name;

    private String description;
}
