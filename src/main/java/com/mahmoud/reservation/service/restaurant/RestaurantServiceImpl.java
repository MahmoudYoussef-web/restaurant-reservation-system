package com.mahmoud.reservation.service.restaurant;

import com.mahmoud.reservation.dto.common.PageResponse;
import com.mahmoud.reservation.dto.restaurant.RestaurantResponse;
import com.mahmoud.reservation.entity.Restaurant;
import com.mahmoud.reservation.exception.BadRequestException;
import com.mahmoud.reservation.exception.ResourceNotFoundException;
import com.mahmoud.reservation.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RestaurantServiceImpl implements RestaurantService {

    private final RestaurantRepository restaurantRepository;

    @Override
    public PageResponse<RestaurantResponse> browseRestaurants(String search, int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new BadRequestException("Invalid pagination parameters (page >= 0, 1 <= size <= 100)");
        }

        PageRequest pageable = PageRequest.of(page, size);
        Page<Restaurant> result;

        if (search == null || search.isBlank()) {
            result = restaurantRepository.findAll(pageable);
        } else {
            String q = search.trim();
            result = restaurantRepository
                    .findByNameContainingIgnoreCaseOrLocationContainingIgnoreCaseOrCuisineContainingIgnoreCase(
                            q, q, q, pageable);
        }

        return PageResponse.<RestaurantResponse>builder()
                .content(result.getContent().stream().map(this::toResponse).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .last(result.isLast())
                .build();
    }

    @Override
    public RestaurantResponse getRestaurantById(Long id) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found"));
        return toResponse(restaurant);
    }

    private RestaurantResponse toResponse(Restaurant r) {
        return RestaurantResponse.builder()
                .id(r.getId())
                .name(r.getName())
                .location(r.getLocation())
                .openingTime(r.getOpeningTime() != null ? r.getOpeningTime().toString() : null)
                .closingTime(r.getClosingTime() != null ? r.getClosingTime().toString() : null)
                .phone(r.getPhone())
                .description(r.getDescription())
                .imageUrl(r.getImageUrl())
                .cuisine(r.getCuisine())
                .priceRange(r.getPriceRange())
                .build();
    }
}
