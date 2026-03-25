package com.mahmoud.reservation.mapper;

import com.mahmoud.reservation.dto.user.UserResponse;
import com.mahmoud.reservation.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .roles(
                        user.getUserRoles().stream()
                                .map(ur -> ur.getRole().getName().name())
                                .toList()
                )
                .build();
    }
}