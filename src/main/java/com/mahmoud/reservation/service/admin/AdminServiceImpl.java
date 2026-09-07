package com.mahmoud.reservation.service.admin;

import com.mahmoud.reservation.dto.admin.*;
import com.mahmoud.reservation.dto.common.MessageResponse;
import com.mahmoud.reservation.dto.common.PageResponse;
import com.mahmoud.reservation.dto.menu.MenuCategoryRequest;
import com.mahmoud.reservation.dto.menu.MenuCategoryResponse;
import com.mahmoud.reservation.dto.menu.MenuItemRequest;
import com.mahmoud.reservation.dto.menu.MenuItemResponse;
import com.mahmoud.reservation.dto.restaurant.RestaurantResponse;
import com.mahmoud.reservation.dto.table.DiningTableResponse;
import com.mahmoud.reservation.entity.*;
import com.mahmoud.reservation.enums.ReservationStatus;
import com.mahmoud.reservation.enums.RoleName;
import com.mahmoud.reservation.enums.TableStatus;
import com.mahmoud.reservation.exception.BadRequestException;
import com.mahmoud.reservation.exception.ConflictException;
import com.mahmoud.reservation.exception.ResourceNotFoundException;
import com.mahmoud.reservation.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminServiceImpl implements AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminServiceImpl.class);

    private final RestaurantRepository restaurantRepository;
    private final DiningTableRepository diningTableRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ReservationRepository reservationRepository;
    private final MenuCategoryRepository menuCategoryRepository;
    private final MenuItemRepository menuItemRepository;

    private String formatTime(LocalTime time) {
        return time != null ? time.toString() : null;
    }

    private LocalTime parseTime(String value, String field) {
        if (value == null) {
            return null;
        }
        try {
            return LocalTime.parse(value);
        } catch (java.time.format.DateTimeParseException e) {
            throw new BadRequestException("Invalid " + field + " format, expected HH:mm");
        }
    }

    private RestaurantResponse toRestaurantResponse(Restaurant r) {
        return RestaurantResponse.builder()
                .id(r.getId())
                .name(r.getName())
                .location(r.getLocation())
                .openingTime(formatTime(r.getOpeningTime()))
                .closingTime(formatTime(r.getClosingTime()))
                .phone(r.getPhone())
                .description(r.getDescription())
                .imageUrl(r.getImageUrl())
                .cuisine(r.getCuisine())
                .priceRange(r.getPriceRange())
                .build();
    }

    private DiningTableResponse toDiningTableResponse(DiningTable t) {
        return DiningTableResponse.builder()
                .id(t.getId())
                .tableNumber(t.getTableNumber())
                .capacity(t.getCapacity())
                .tableStatus(t.getTableStatus().name())
                .restaurantId(t.getRestaurant().getId())
                .build();
    }

    @Override
    public RestaurantResponse createRestaurant(CreateRestaurantRequest request) {
        if (restaurantRepository.existsByName(request.getName())) {
            throw new ConflictException("Restaurant already exists");
        }

        Restaurant restaurant = Restaurant.builder()
                .name(request.getName())
                .location(request.getLocation())
                .openingTime(parseTime(request.getOpeningTime(), "openingTime"))
                .closingTime(parseTime(request.getClosingTime(), "closingTime"))
                .phone(request.getPhone())
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .cuisine(request.getCuisine())
                .priceRange(request.getPriceRange())
                .build();

        Restaurant saved = restaurantRepository.save(restaurant);
        return toRestaurantResponse(saved);
    }

    @Override
    public RestaurantResponse updateRestaurant(Long id, CreateRestaurantRequest request) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found"));

        if (restaurantRepository.existsByNameAndIdNot(request.getName(), id)) {
            throw new ConflictException("Restaurant name already in use");
        }

        restaurant.setName(request.getName());
        restaurant.setLocation(request.getLocation());
        restaurant.setOpeningTime(parseTime(request.getOpeningTime(), "openingTime"));
        restaurant.setClosingTime(parseTime(request.getClosingTime(), "closingTime"));
        restaurant.setPhone(request.getPhone());
        restaurant.setDescription(request.getDescription());
        restaurant.setImageUrl(request.getImageUrl());
        restaurant.setCuisine(request.getCuisine());
        restaurant.setPriceRange(request.getPriceRange());

        Restaurant saved = restaurantRepository.save(restaurant);
        log.info("Updated restaurant {}: {}", id, request.getName());
        return toRestaurantResponse(saved);
    }

    @Override
    public void deleteRestaurant(Long id) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found"));
        restaurant.setDeleted(true);
        restaurantRepository.save(restaurant);
        log.info("Soft deleted restaurant {}", id);
    }

    @Override
    public DiningTableResponse createDiningTable(CreateDiningTableRequest request) {
        Restaurant restaurant = restaurantRepository.findById(request.getRestaurantId())
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found"));

        if (diningTableRepository.existsByRestaurantIdAndTableNumber(
                request.getRestaurantId(), request.getTableNumber()
        )) {
            throw new ConflictException("Table already exists");
        }

        DiningTable table = DiningTable.builder()
                .restaurant(restaurant)
                .tableNumber(request.getTableNumber())
                .capacity(request.getCapacity())
                .build();

        DiningTable saved = diningTableRepository.save(table);
        return toDiningTableResponse(saved);
    }

    @Override
    public DiningTableResponse updateDiningTable(Long id, UpdateDiningTableRequest request) {
        DiningTable table = diningTableRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dining table not found"));

        if (!table.getTableNumber().equals(request.getTableNumber())
                && diningTableRepository.existsByRestaurantIdAndTableNumber(
                        table.getRestaurant().getId(), request.getTableNumber())) {
            throw new ConflictException("Table number already exists in this restaurant");
        }

        table.setTableNumber(request.getTableNumber());
        table.setCapacity(request.getCapacity());

        DiningTable saved = diningTableRepository.save(table);
        log.info("Updated dining table {}: tableNumber={}", id, request.getTableNumber());
        return toDiningTableResponse(saved);
    }

    @Override
    public void deleteDiningTable(Long id) {
        DiningTable table = diningTableRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dining table not found"));
        table.setDeleted(true);
        diningTableRepository.save(table);
        log.info("Soft deleted dining table {}", id);
    }

    @Override
    public DiningTableResponse updateTableStatus(Long id, UpdateTableStatusRequest request) {
        DiningTable table = diningTableRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dining table not found"));

        TableStatus status;
        try {
            status = TableStatus.valueOf(request.getTableStatus().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid table status");
        }

        table.setTableStatus(status);
        DiningTable saved = diningTableRepository.save(table);
        log.info("Updated table {} status to {}", id, status);
        return toDiningTableResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RestaurantResponse> getAllRestaurants(int page, int size) {
        validatePagination(page, size);

        PageRequest pageable = PageRequest.of(page, size);
        Page<Restaurant> result = restaurantRepository.findAll(pageable);

        return PageResponse.<RestaurantResponse>builder()
                .content(result.getContent().stream().map(this::toRestaurantResponse).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .last(result.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DiningTableResponse> getTablesByRestaurant(Long restaurantId, int page, int size) {
        validatePagination(page, size);

        restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found"));

        PageRequest pageable = PageRequest.of(page, size);
        Page<DiningTable> result = diningTableRepository.findByRestaurantId(restaurantId, pageable);

        return PageResponse.<DiningTableResponse>builder()
                .content(result.getContent().stream().map(this::toDiningTableResponse).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .last(result.isLast())
                .build();
    }

    @Override
    public MessageResponse assignOwnerRole(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Role ownerRole = roleRepository.findByName(RoleName.ROLE_OWNER)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));

        boolean alreadyOwner = user.getUserRoles().stream()
                .anyMatch(ur -> ur.getRole().getName() == RoleName.ROLE_OWNER);

        if (!alreadyOwner) {
            UserRole userRole = UserRole.builder()
                    .user(user)
                    .role(ownerRole)
                    .build();
            user.getUserRoles().add(userRole);
            userRepository.save(user);
            log.info("Assigned OWNER role to user {}", userId);
        }

        return new MessageResponse("Owner role assigned successfully");
    }

    @Override
    public MessageResponse cancelReservationByAdmin(Long reservationId) {
        Reservation reservation = reservationRepository.findByIdWithLock(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found"));

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new BadRequestException("Reservation already cancelled");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        log.info("Admin cancelled reservation {}", reservationId);
        return new MessageResponse("Reservation cancelled successfully");
    }

    @Override
    public MessageResponse approveReservation(Long reservationId) {
        Reservation reservation = reservationRepository.findByIdWithLock(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found"));

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new BadRequestException("Only pending reservations can be approved");
        }

        reservation.setStatus(ReservationStatus.APPROVED);
        log.info("Admin approved reservation {}", reservationId);
        return new MessageResponse("Reservation approved successfully");
    }

    @Override
    public MessageResponse rejectReservation(Long reservationId) {
        Reservation reservation = reservationRepository.findByIdWithLock(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found"));

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new BadRequestException("Only pending reservations can be rejected");
        }

        reservation.setStatus(ReservationStatus.REJECTED);
        log.info("Admin rejected reservation {}", reservationId);
        return new MessageResponse("Reservation rejected successfully");
    }

    @Override
    public MenuCategoryResponse createCategory(MenuCategoryRequest request) {
        if (menuCategoryRepository.existsByName(request.getName())) {
            throw new ConflictException("Category already exists");
        }

        MenuCategory category = MenuCategory.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();

        MenuCategory saved = menuCategoryRepository.save(category);

        return MenuCategoryResponse.builder()
                .id(saved.getId())
                .name(saved.getName())
                .description(saved.getDescription())
                .build();
    }

    @Override
    public MenuCategoryResponse updateCategory(Long id, MenuCategoryRequest request) {
        MenuCategory category = menuCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        if (!category.getName().equals(request.getName())
                && menuCategoryRepository.existsByName(request.getName())) {
            throw new ConflictException("Category already exists");
        }

        category.setName(request.getName());
        category.setDescription(request.getDescription());

        MenuCategory saved = menuCategoryRepository.save(category);

        return MenuCategoryResponse.builder()
                .id(saved.getId())
                .name(saved.getName())
                .description(saved.getDescription())
                .build();
    }

    @Override
    public void deleteCategory(Long id) {
        MenuCategory category = menuCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        category.setDeleted(true);
        menuCategoryRepository.save(category);
        log.info("Soft deleted menu category {}", id);
    }

    @Override
    public MenuItemResponse createMenuItem(MenuItemRequest request) {
        MenuCategory category = menuCategoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        MenuItem item = MenuItem.builder()
                .category(category)
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .imageUrl(request.getImageUrl())
                .available(request.getAvailable() != null ? request.getAvailable() : true)
                .build();

        MenuItem saved = menuItemRepository.save(item);

        return MenuItemResponse.builder()
                .id(saved.getId())
                .categoryId(category.getId())
                .name(saved.getName())
                .description(saved.getDescription())
                .price(saved.getPrice())
                .imageUrl(saved.getImageUrl())
                .available(saved.isAvailable())
                .build();
    }

    @Override
    public MenuItemResponse updateMenuItem(Long id, MenuItemRequest request) {
        MenuItem item = menuItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found"));

        MenuCategory category = menuCategoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        item.setCategory(category);
        item.setName(request.getName());
        item.setDescription(request.getDescription());
        item.setPrice(request.getPrice());
        item.setImageUrl(request.getImageUrl());
        item.setAvailable(request.getAvailable() != null ? request.getAvailable() : item.isAvailable());

        MenuItem saved = menuItemRepository.save(item);

        return MenuItemResponse.builder()
                .id(saved.getId())
                .categoryId(category.getId())
                .name(saved.getName())
                .description(saved.getDescription())
                .price(saved.getPrice())
                .imageUrl(saved.getImageUrl())
                .available(saved.isAvailable())
                .build();
    }

    @Override
    public void deleteMenuItem(Long id) {
        MenuItem item = menuItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found"));
        item.setDeleted(true);
        menuItemRepository.save(item);
        log.info("Soft deleted menu item {}", id);
    }

    private void validatePagination(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new BadRequestException("Invalid pagination parameters (page >= 0, 1 <= size <= 100)");
        }
    }
}
