package com.mahmoud.reservation.service.user;

import com.mahmoud.reservation.dto.user.UpdateUserRequest;
import com.mahmoud.reservation.dto.user.UserResponse;
import com.mahmoud.reservation.entity.User;
import com.mahmoud.reservation.exception.ResourceNotFoundException;
import com.mahmoud.reservation.mapper.UserMapper;
import com.mahmoud.reservation.repository.UserRepository;
import com.mahmoud.reservation.security.user.ShopUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public UserResponse getCurrentUser() {

        ShopUserDetails userDetails = (ShopUserDetails) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    public UserResponse updateProfile(UpdateUserRequest request) {

        ShopUserDetails userDetails = (ShopUserDetails) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName().trim());
        }

        if (request.getLastName() != null) {
            user.setLastName(request.getLastName().trim());
        }

        if (request.getEmail() != null) {
            user.setEmail(request.getEmail().toLowerCase().trim());
        }

        return userMapper.toResponse(user);
    }
}