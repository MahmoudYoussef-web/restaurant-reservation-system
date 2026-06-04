package com.mahmoud.reservation.dto.admin;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTableStatusRequest {

    @NotBlank
    private String tableStatus;
}
