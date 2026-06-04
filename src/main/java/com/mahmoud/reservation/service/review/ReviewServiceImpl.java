package com.mahmoud.reservation.service.review;

import com.mahmoud.reservation.dto.common.PageResponse;
import com.mahmoud.reservation.dto.review.CreateReviewRequest;
import com.mahmoud.reservation.dto.review.ReviewResponse;
import com.mahmoud.reservation.entity.Restaurant;
import com.mahmoud.reservation.entity.Review;
import com.mahmoud.reservation.entity.User;
import com.mahmoud.reservation.enums.ReservationStatus;
import com.mahmoud.reservation.exception.BadRequestException;
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

import java.util.List;

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
        restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<Long> reservationIds = reservationRepository.findByUserIdWithDetails(userId)
                .stream()
                .filter(r -> r.getTable().getRestaurant().getId().equals(restaurantId))
                .filter(r -> r.getStatus() == ReservationStatus.COMPLETED)
                .map(r -> r.getId())
                .toList();

        if (reservationIds.isEmpty()) {
            throw new BadRequestException("You must have a completed reservation to review this restaurant");
        }

        Review review = Review.builder()
                .user(user)
                .restaurant(restaurantRepository.getReferenceById(restaurantId))
                .rating(request.getRating())
                .comment(request.getComment())
                .build();

        Review saved = reviewRepository.save(review);

        return ReviewResponse.builder()
                .id(saved.getId())
                .userId(user.getId())
                .userName(user.getFirstName() + " " + user.getLastName())
                .restaurantId(restaurantId)
                .rating(saved.getRating())
                .comment(saved.getComment())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getReviewsByRestaurant(Long restaurantId, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);
        Page<Review> result = reviewRepository.findByRestaurantIdOrderByCreatedAtDesc(restaurantId, pageable);

        return PageResponse.<ReviewResponse>builder()
                .content(result.getContent().stream()
                        .map(r -> ReviewResponse.builder()
                                .id(r.getId())
                                .userId(r.getUser().getId())
                                .userName(r.getUser().getFirstName() + " " + r.getUser().getLastName())
                                .restaurantId(restaurantId)
                                .rating(r.getRating())
                                .comment(r.getComment())
                                .createdAt(r.getCreatedAt())
                                .build())
                        .toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .last(result.isLast())
                .build();
    }
}
