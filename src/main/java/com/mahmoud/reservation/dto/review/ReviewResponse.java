package com.mahmoud.reservation.dto.review;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {

    private Long id;
    private Long userId;
    private String userName;
    private Long restaurantId;
    private Integer rating;
    private String comment;
    private Instant createdAt;
}
