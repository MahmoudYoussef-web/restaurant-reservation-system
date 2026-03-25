package com.mahmoud.reservation.service.auth;

import com.mahmoud.reservation.dto.auth.AuthResponse;
import com.mahmoud.reservation.dto.auth.LoginRequest;
import com.mahmoud.reservation.dto.auth.RegisterRequest;
import com.mahmoud.reservation.entity.RefreshToken;
import com.mahmoud.reservation.entity.Role;
import com.mahmoud.reservation.entity.User;
import com.mahmoud.reservation.entity.UserRole;
import com.mahmoud.reservation.enums.RoleName;
import com.mahmoud.reservation.enums.UserStatus;
import com.mahmoud.reservation.exception.BadRequestException;
import com.mahmoud.reservation.exception.ResourceNotFoundException;
import com.mahmoud.reservation.repository.RefreshTokenRepository;
import com.mahmoud.reservation.repository.RoleRepository;
import com.mahmoud.reservation.repository.UserRepository;
import com.mahmoud.reservation.repository.UserRoleRepository;
import com.mahmoud.reservation.security.jwt.JwtUtils;
import com.mahmoud.reservation.security.user.ShopUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {

        String email = request.getEmail().toLowerCase().trim();

        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("Email already exists");
        }

        User user = User.builder()
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .email(email)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .status(UserStatus.PENDING_VERIFICATION)
                .emailVerified(false)
                .build();

        userRepository.save(user);

        Role role = roleRepository.findByName(RoleName.ROLE_USER)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));

        userRoleRepository.save(
                UserRole.builder()
                        .user(user)
                        .role(role)
                        .build()
        );

        return generateTokens(user);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {

        String email = request.getEmail().toLowerCase().trim();

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        email,
                        request.getPassword()
                )
        );

        ShopUserDetails userDetails = (ShopUserDetails) authentication.getPrincipal();

        User user = userRepository.findByEmailWithRoles(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return generateTokens(userDetails, user);
    }

    private AuthResponse generateTokens(ShopUserDetails userDetails, User user) {

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );

        String accessToken = jwtUtils.generateToken(authentication);

        String rawRefreshToken = UUID.randomUUID().toString();
        String hashedToken = passwordEncoder.encode(rawRefreshToken);
        String tokenId = UUID.randomUUID().toString();

        RefreshToken token = RefreshToken.builder()
                .tokenId(tokenId)
                .user(user)
                .tokenHash(hashedToken)
                .expiresAt(Instant.now().plusSeconds(604800))
                .revoked(false)
                .build();

        refreshTokenRepository.save(token);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(rawRefreshToken)
                .refreshTokenId(tokenId)
                .build();
    }

    private AuthResponse generateTokens(User user) {

        List<SimpleGrantedAuthority> authorities = userRoleRepository.findByUser(user)
                .stream()
                .map(ur -> new SimpleGrantedAuthority(ur.getRole().getName().name()))
                .toList();

        ShopUserDetails userDetails = new ShopUserDetails(
                user.getId(),
                user.getEmail(),
                user.getPasswordHash(),
                true,
                true,
                authorities
        );

        return generateTokens(userDetails, user);
    }

    @Override
    @Transactional
    public AuthResponse refreshToken(String refreshToken, String tokenId) {

        RefreshToken token = refreshTokenRepository.findByTokenId(tokenId)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid refresh token"));

        if (!passwordEncoder.matches(refreshToken, token.getTokenHash())) {
            throw new BadRequestException("Invalid refresh token");
        }

        if (!token.isValid()) {
            throw new BadRequestException("Refresh token expired or revoked");
        }

        token.setRevoked(true);
        token.setRevokedAt(Instant.now());

        return generateTokens(token.getUser());
    }

    @Override
    @Transactional
    public void logout(String tokenId) {

        RefreshToken token = refreshTokenRepository.findByTokenId(tokenId)
                .orElseThrow(() -> new ResourceNotFoundException("Token not found"));

        token.setRevoked(true);
        token.setRevokedAt(Instant.now());
    }
}