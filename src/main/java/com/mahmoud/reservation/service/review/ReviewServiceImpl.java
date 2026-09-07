package com.mahmoud.reservation.service.review;

import com.mahmoud.reservation.dto.common.PageResponse;
import com.mahmoud.reservation.dto.review.CreateReviewRequest;
import com.mahmoud.reservation.dto.review.ReviewResponse;
import com.mahmoud.reservation.entity.Restaurant;
import com.mahmoud.reservation.entity.Review;
import com.mahmoud.reservation.entity.User;
import com.mahmoud.reservation.enums.ReservationStatus;
import com.mahmoud.reservation.exception.BadRequestException;
import com.mahmoud.reservation.exception.ConflictException;
import com.mahmoud.reservation.exception.ForbiddenException;
import com.mahmoud.reservation.exception.ResourceNotFoundException;
import com.mahmoud.reservation.repository.ReservationRepository;
import com.mahmoud.reservation.repository.RestaurantRepository;
import com.mahmoud.reservation.repository.ReviewRepository;
import com.mahmoud.reservation.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final RestaurantRepository restaurantRepository;
    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;

    @Override
    public ReviewResponse createReview(Long restaurantId, CreateReviewRequest request, Long userId) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        long completedStays = reservationRepository.countByUserIdAndRestaurantIdAndStatus(
                userId, restaurantId, ReservationStatus.COMPLETED);

        if (completedStays == 0) {
            throw new BadRequestException("You must have a completed reservation to review this restaurant");
        }

        if (reviewRepository.existsByUserIdAndRestaurantId(userId, restaurantId)) {
            throw new ConflictException("You have already reviewed this restaurant");
        }

        Review review = Review.builder()
                .user(user)
                .restaurant(restaurant)
                .rating(request.getRating())
                .comment(request.getComment())
                .build();

        Review saved = reviewRepository.save(review);

        return toResponse(saved, userId, restaurantId);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getReviewsByRestaurant(Long restaurantId, int page, int size) {
        validatePagination(page, size);
        restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found"));

        PageRequest pageable = PageRequest.of(page, size);
        Page<Review> result = reviewRepository.findByRestaurantIdOrderByCreatedAtDesc(restaurantId, pageable);

        return PageResponse.<ReviewResponse>builder()
                .content(result.getContent().stream()
                        .map(r -> toResponse(r, r.getUser().getId(), restaurantId))
                        .toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .last(result.isLast())
                .build();
    }

    @Override
    public ReviewResponse updateReview(Long reviewId, CreateReviewRequest request, Long userId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        if (!review.getUser().getId().equals(userId)) {
            throw new ForbiddenException("You can only update your own reviews");
        }

        review.setRating(request.getRating());
        review.setComment(request.getComment());

        return toResponse(reviewRepository.save(review),
                userId, review.getRestaurant().getId());
    }

    @Override
    public void deleteReview(Long reviewId, Long userId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        if (!review.getUser().getId().equals(userId)) {
            throw new ForbiddenException("You can only delete your own reviews");
        }

        review.setDeleted(true);
        reviewRepository.save(review);
    }

    private ReviewResponse toResponse(Review review, Long userId, Long restaurantId) {
        return ReviewResponse.builder()
                .id(review.getId())
                .userId(userId)
                .userName(review.getUser().getFirstName() + " " + review.getUser().getLastName())
                .restaurantId(restaurantId)
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .build();
    }

    private void validatePagination(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new BadRequestException("Invalid pagination parameters (page >= 0, 1 <= size <= 100)");
        }
    }
}
