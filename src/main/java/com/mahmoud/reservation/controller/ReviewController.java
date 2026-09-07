package com.mahmoud.reservation.controller;

import com.mahmoud.reservation.dto.common.MessageResponse;
import com.mahmoud.reservation.dto.common.PageResponse;
import com.mahmoud.reservation.dto.review.CreateReviewRequest;
import com.mahmoud.reservation.dto.review.ReviewResponse;
import com.mahmoud.reservation.exception.UnauthorizedException;
import com.mahmoud.reservation.security.user.ShopUserDetails;
import com.mahmoud.reservation.service.review.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "Create review for restaurant")
    @PostMapping("/restaurants/{restaurantId}/reviews")
    public ResponseEntity<ReviewResponse> createReview(
            @PathVariable Long restaurantId,
            @Valid @RequestBody CreateReviewRequest request
    ) {
        Long userId = getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reviewService.createReview(restaurantId, request, userId));
    }

    @Operation(summary = "Get reviews by restaurant (paginated)")
    @GetMapping("/restaurants/{restaurantId}/reviews")
    public ResponseEntity<PageResponse<ReviewResponse>> getReviews(
            @PathVariable Long restaurantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(reviewService.getReviewsByRestaurant(restaurantId, page, size));
    }

    @Operation(summary = "Update own review")
    @PutMapping("/reviews/{reviewId}")
    public ResponseEntity<ReviewResponse> updateReview(
            @PathVariable Long reviewId,
            @Valid @RequestBody CreateReviewRequest request
    ) {
        return ResponseEntity.ok(reviewService.updateReview(reviewId, request, getCurrentUserId()));
    }

    @Operation(summary = "Delete own review")
    @DeleteMapping("/reviews/{reviewId}")
    public ResponseEntity<MessageResponse> deleteReview(@PathVariable Long reviewId) {
        reviewService.deleteReview(reviewId, getCurrentUserId());
        return ResponseEntity.ok(new MessageResponse("Review deleted successfully"));
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof ShopUserDetails userDetails)) {
            throw new UnauthorizedException("Unauthorized");
        }
        return userDetails.getId();
    }
}
