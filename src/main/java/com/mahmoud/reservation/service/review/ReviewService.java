package com.mahmoud.reservation.service.review;

import com.mahmoud.reservation.dto.common.PageResponse;
import com.mahmoud.reservation.dto.review.CreateReviewRequest;
import com.mahmoud.reservation.dto.review.ReviewResponse;

public interface ReviewService {

    ReviewResponse createReview(Long restaurantId, CreateReviewRequest request, Long userId);

    PageResponse<ReviewResponse> getReviewsByRestaurant(Long restaurantId, int page, int size);

    ReviewResponse updateReview(Long reviewId, CreateReviewRequest request, Long userId);

    void deleteReview(Long reviewId, Long userId);
}
