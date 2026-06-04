package com.mahmoud.reservation.controller;

import com.mahmoud.reservation.dto.common.MessageResponse;
import com.mahmoud.reservation.dto.user.ChangePasswordRequest;
import com.mahmoud.reservation.dto.user.UpdateUserRequest;
import com.mahmoud.reservation.dto.user.UserResponse;
import com.mahmoud.reservation.service.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "Get current user profile")
    @GetMapping("/me")
    public UserResponse getCurrentUser() {
        return userService.getCurrentUser();
    }

    @Operation(summary = "Update user profile")
    @PutMapping("/me")
    public UserResponse update(@Valid @RequestBody UpdateUserRequest request) {
        return userService.updateProfile(request);
    }

    @Operation(summary = "Change password")
    @PutMapping("/me/password")
    public MessageResponse changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(request);
        return new MessageResponse("Password changed successfully");
    }
}