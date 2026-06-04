package com.mahmoud.reservation.service.user;

import com.mahmoud.reservation.dto.user.ChangePasswordRequest;
import com.mahmoud.reservation.dto.user.UpdateUserRequest;
import com.mahmoud.reservation.dto.user.UserResponse;

public interface UserService {

    UserResponse getCurrentUser();

    UserResponse updateProfile(UpdateUserRequest request);

    void changePassword(ChangePasswordRequest request);
}