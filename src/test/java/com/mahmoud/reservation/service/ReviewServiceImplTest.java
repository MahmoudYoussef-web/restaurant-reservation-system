package com.mahmoud.reservation.service;

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
import com.mahmoud.reservation.repository.ReservationRepository;
import com.mahmoud.reservation.repository.RestaurantRepository;
import com.mahmoud.reservation.repository.ReviewRepository;
import com.mahmoud.reservation.repository.UserRepository;
import com.mahmoud.reservation.service.review.ReviewServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private RestaurantRepository restaurantRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ReservationRepository reservationRepository;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    @Test
    void createReview_shouldSucceed_withCompletedStay() {
        Restaurant restaurant = Restaurant.builder().id(1L).name("Casa").build();
        User user = User.builder().firstName("Mona").lastName("Ali").email("mona@test.com").build();
        user.setId(7L);

        when(restaurantRepository.findById(1L)).thenReturn(Optional.of(restaurant));
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(reservationRepository.countByUserIdAndRestaurantIdAndStatus(
                7L, 1L, ReservationStatus.COMPLETED)).thenReturn(1L);
        when(reviewRepository.existsByUserIdAndRestaurantId(7L, 1L)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenAnswer(i -> {
            Review r = i.getArgument(0);
            r.setId(3L);
            return r;
        });

        ReviewResponse res = reviewService.createReview(1L,
                CreateReviewRequest.builder().rating(5).comment("Amazing!").build(), 7L);

        assertThat(res.getRating()).isEqualTo(5);
        assertThat(res.getUserName()).isEqualTo("Mona Ali");
    }

    @Test
    void createReview_shouldThrow_withoutCompletedStay() {
        when(restaurantRepository.findById(1L))
                .thenReturn(Optional.of(Restaurant.builder().id(1L).build()));
        when(userRepository.findById(7L))
                .thenReturn(Optional.of(User.builder().email("a@b.com").build()));
        when(reservationRepository.countByUserIdAndRestaurantIdAndStatus(
                7L, 1L, ReservationStatus.COMPLETED)).thenReturn(0L);

        assertThatThrownBy(() -> reviewService.createReview(1L,
                CreateReviewRequest.builder().rating(4).build(), 7L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("completed reservation");
    }

    @Test
    void createReview_shouldThrow_onDuplicate() {
        when(restaurantRepository.findById(1L))
                .thenReturn(Optional.of(Restaurant.builder().id(1L).build()));
        when(userRepository.findById(7L))
                .thenReturn(Optional.of(User.builder().email("a@b.com").build()));
        when(reservationRepository.countByUserIdAndRestaurantIdAndStatus(
                7L, 1L, ReservationStatus.COMPLETED)).thenReturn(2L);
        when(reviewRepository.existsByUserIdAndRestaurantId(7L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> reviewService.createReview(1L,
                CreateReviewRequest.builder().rating(4).build(), 7L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already reviewed");
    }

    @Test
    void updateReview_shouldThrow_whenNotOwner() {
        User owner = User.builder().email("o@x.com").build();
        owner.setId(1L);
        Review review = Review.builder().id(9L).user(owner)
                .restaurant(Restaurant.builder().id(1L).build()).rating(3).build();
        when(reviewRepository.findById(9L)).thenReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.updateReview(9L,
                CreateReviewRequest.builder().rating(5).build(), 2L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void getReviews_shouldValidatePagination() {
        assertThatThrownBy(() -> reviewService.getReviewsByRestaurant(1L, 0, 101))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void getReviews_shouldReturnPagedResults() {
        User user = User.builder().firstName("Mona").lastName("Ali").email("m@t.com").build();
        user.setId(7L);
        Review review = Review.builder().id(3L).user(user)
                .restaurant(Restaurant.builder().id(1L).build()).rating(5).comment("Great").build();

        when(restaurantRepository.findById(1L))
                .thenReturn(Optional.of(Restaurant.builder().id(1L).build()));
        when(reviewRepository.findByRestaurantIdOrderByCreatedAtDesc(
                org.mockito.ArgumentMatchers.eq(1L), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(review)));

        PageResponse<ReviewResponse> res = reviewService.getReviewsByRestaurant(1L, 0, 10);

        assertThat(res.getContent()).hasSize(1);
        assertThat(res.getContent().get(0).getRating()).isEqualTo(5);
    }
}
