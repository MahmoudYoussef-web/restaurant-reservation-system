package com.mahmoud.reservation.service;

import com.mahmoud.reservation.dto.user.UpdateUserRequest;
import com.mahmoud.reservation.dto.user.UserResponse;
import com.mahmoud.reservation.entity.User;
import com.mahmoud.reservation.exception.ResourceNotFoundException;
import com.mahmoud.reservation.mapper.UserMapper;
import com.mahmoud.reservation.repository.UserRepository;
import com.mahmoud.reservation.security.user.ShopUserDetails;
import com.mahmoud.reservation.service.user.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        ShopUserDetails userDetails = new ShopUserDetails(
                1L, "john@example.com", "hash", true, true, List.of()
        );

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userDetails);

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void getCurrentUser_shouldReturnUser() {
        User user = User.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .build();
        user.setId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserResponse expectedResponse = UserResponse.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .build();

        when(userMapper.toResponse(user)).thenReturn(expectedResponse);

        UserResponse response = userService.getCurrentUser();

        assertThat(response.getFirstName()).isEqualTo("John");
    }

    @Test
    void getCurrentUser_shouldThrow_whenNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getCurrentUser())
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateProfile_shouldUpdateFields() {
        UpdateUserRequest request = UpdateUserRequest.builder()
                .firstName("Jane")
                .lastName("Smith")
                .email("jane@example.com")
                .build();

        User user = User.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .build();
        user.setId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserResponse expectedResponse = UserResponse.builder()
                .firstName("Jane")
                .lastName("Smith")
                .email("jane@example.com")
                .build();

        when(userMapper.toResponse(user)).thenReturn(expectedResponse);

        UserResponse response = userService.updateProfile(request);

        assertThat(response.getFirstName()).isEqualTo("Jane");
        assertThat(response.getLastName()).isEqualTo("Smith");
    }

    @Test
    void updateProfile_shouldNotUpdateNullFields() {
        UpdateUserRequest request = UpdateUserRequest.builder()
                .firstName("Jane")
                .build();

        User user = User.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .build();
        user.setId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserResponse expectedResponse = UserResponse.builder()
                .firstName("Jane")
                .lastName("Doe")
                .email("john@example.com")
                .build();

        when(userMapper.toResponse(user)).thenReturn(expectedResponse);

        UserResponse response = userService.updateProfile(request);

        assertThat(response.getFirstName()).isEqualTo("Jane");
        assertThat(response.getLastName()).isEqualTo("Doe");
    }
}
