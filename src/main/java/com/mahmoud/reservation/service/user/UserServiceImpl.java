package com.mahmoud.reservation.service.user;

import com.mahmoud.reservation.dto.user.ChangePasswordRequest;
import com.mahmoud.reservation.dto.user.UpdateUserRequest;
import com.mahmoud.reservation.dto.user.UserResponse;
import com.mahmoud.reservation.entity.User;
import com.mahmoud.reservation.exception.BadRequestException;
import com.mahmoud.reservation.exception.ConflictException;
import com.mahmoud.reservation.exception.ResourceNotFoundException;
import com.mahmoud.reservation.mapper.UserMapper;
import com.mahmoud.reservation.repository.UserRepository;
import com.mahmoud.reservation.security.user.ShopUserDetails;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

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
            String normalizedEmail = request.getEmail().toLowerCase().trim();
            if (!normalizedEmail.equals(user.getEmail()) && userRepository.existsByEmail(normalizedEmail)) {
                throw new ConflictException("Email already in use");
            }
            user.setEmail(normalizedEmail);
        }

        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordRequest request) {

        ShopUserDetails userDetails = (ShopUserDetails) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        log.info("Password changed for user {}", user.getEmail());
    }
}