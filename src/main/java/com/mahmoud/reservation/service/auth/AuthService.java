package com.mahmoud.reservation.service.auth;

import com.mahmoud.reservation.dto.auth.AuthResponse;
import com.mahmoud.reservation.dto.auth.LoginRequest;
import com.mahmoud.reservation.dto.auth.RegisterRequest;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refreshToken(String refreshToken, String tokenId);

    void logout(String tokenId);
}