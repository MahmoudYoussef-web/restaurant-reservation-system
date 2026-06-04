package com.mahmoud.reservation.service.auth;

import com.mahmoud.reservation.dto.auth.*;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refreshToken(String refreshToken, String tokenId);

    void logout(String tokenId);

    void forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);
}