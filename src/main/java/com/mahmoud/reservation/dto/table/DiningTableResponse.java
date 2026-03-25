package com.mahmoud.reservation.dto.table;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiningTableResponse {

    private Long id;
    private Integer tableNumber;
    private Integer capacity;
    private Long restaurantId;
}