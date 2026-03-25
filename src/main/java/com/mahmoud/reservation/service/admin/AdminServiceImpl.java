package com.mahmoud.reservation.service.admin;

import com.mahmoud.reservation.dto.admin.CreateDiningTableRequest;
import com.mahmoud.reservation.dto.admin.CreateRestaurantRequest;
import com.mahmoud.reservation.dto.common.PageResponse;
import com.mahmoud.reservation.dto.restaurant.RestaurantResponse;
import com.mahmoud.reservation.dto.table.DiningTableResponse;
import com.mahmoud.reservation.entity.DiningTable;
import com.mahmoud.reservation.entity.Restaurant;
import com.mahmoud.reservation.exception.BadRequestException;
import com.mahmoud.reservation.exception.ConflictException;
import com.mahmoud.reservation.exception.ResourceNotFoundException;
import com.mahmoud.reservation.repository.DiningTableRepository;
import com.mahmoud.reservation.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminServiceImpl implements AdminService {

    private final RestaurantRepository restaurantRepository;
    private final DiningTableRepository diningTableRepository;

    @Override
    public RestaurantResponse createRestaurant(CreateRestaurantRequest request) {

        if (restaurantRepository.existsByName(request.getName())) {
            throw new ConflictException("Restaurant already exists");
        }

        Restaurant restaurant = Restaurant.builder()
                .name(request.getName())
                .location(request.getLocation())
                .build();

        Restaurant saved = restaurantRepository.save(restaurant);

        return RestaurantResponse.builder()
                .id(saved.getId())
                .name(saved.getName())
                .location(saved.getLocation())
                .build();
    }

    @Override
    public DiningTableResponse createDiningTable(CreateDiningTableRequest request) {

        Restaurant restaurant = restaurantRepository.findById(request.getRestaurantId())
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found"));

        if (diningTableRepository.existsByRestaurantIdAndTableNumber(
                request.getRestaurantId(),
                request.getTableNumber()
        )) {
            throw new ConflictException("Table already exists");
        }

        DiningTable table = DiningTable.builder()
                .restaurant(restaurant)
                .tableNumber(request.getTableNumber())
                .capacity(request.getCapacity())
                .build();

        DiningTable saved = diningTableRepository.save(table);

        return DiningTableResponse.builder()
                .id(saved.getId())
                .tableNumber(saved.getTableNumber())
                .capacity(saved.getCapacity())
                .restaurantId(restaurant.getId())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RestaurantResponse> getAllRestaurants(int page, int size) {

        validatePagination(page, size);

        PageRequest pageable = PageRequest.of(page, size);
        Page<Restaurant> result = restaurantRepository.findAll(pageable);

        return PageResponse.<RestaurantResponse>builder()
                .content(result.getContent().stream()
                        .map(r -> RestaurantResponse.builder()
                                .id(r.getId())
                                .name(r.getName())
                                .location(r.getLocation())
                                .build())
                        .toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .last(result.isLast())
                .build();
    }@Override
    @Transactional(readOnly = true)
    public PageResponse<DiningTableResponse> getTablesByRestaurant(Long restaurantId, int page, int size) {

        validatePagination(page, size);

        restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found"));

        PageRequest pageable = PageRequest.of(page, size);
        Page<DiningTable> result = diningTableRepository.findByRestaurantId(restaurantId, pageable);

        return PageResponse.<DiningTableResponse>builder()
                .content(result.getContent().stream()
                        .map(t -> DiningTableResponse.builder()
                                .id(t.getId())
                                .tableNumber(t.getTableNumber())
                                .capacity(t.getCapacity())
                                .restaurantId(restaurantId)
                                .build())
                        .toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .last(result.isLast())
                .build();
    }

    private void validatePagination(int page, int size) {
        if (page < 0 || size <= 0) {
            throw new BadRequestException("Invalid pagination parameters");
        }
    }
}