package com.mahmoud.reservation.dto.restaurant;

import lombok.*;

import java.time.LocalTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantResponse {

    private Long id;
    private String name;
    private String location;
    private String openingTime;
    private String closingTime;
    private String phone;
    private String description;
    private String imageUrl;
}