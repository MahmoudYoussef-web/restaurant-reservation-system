package com.mahmoud.reservation.service;

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
import com.mahmoud.reservation.service.auth.AuthServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void register_shouldCreateUserAndReturnTokens() {
        RegisterRequest request = RegisterRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .password("password123")
                .build();

        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedHash");
        when(roleRepository.findByName(RoleName.ROLE_USER))
                .thenReturn(Optional.of(
                        Role.builder().name(RoleName.ROLE_USER).build()
                ));

        User savedUser = User.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .passwordHash("encodedHash")
                .status(UserStatus.PENDING_VERIFICATION)
                .emailVerified(false)
                .build();
        savedUser.setId(1L);

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        UserRole userRole = UserRole.builder()
                .user(savedUser)
                .role(Role.builder().name(RoleName.ROLE_USER).build())
                .build();

        when(userRoleRepository.findByUser(any(User.class))).thenReturn(List.of(userRole));

        when(jwtUtils.generateToken(any(Authentication.class))).thenReturn("access-token");
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse response = authService.register(request);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isNotNull();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("john@example.com");
        assertThat(userCaptor.getValue().getStatus()).isEqualTo(UserStatus.PENDING_VERIFICATION);
    }

    @Test
    void register_shouldThrow_whenEmailExists() {
        RegisterRequest request = RegisterRequest.builder()
                .email("john@example.com")
                .build();

        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Email already exists");

        verify(userRepository, never()).save(any());
    }

    @Test
    void login_shouldReturnTokens() {
        LoginRequest request = LoginRequest.builder()
                .email("john@example.com")
                .password("password123")
                .build();

        ShopUserDetails userDetails = new ShopUserDetails(
                1L, "john@example.com", "hash", true, true, List.of()
        );

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);

        User user = User.builder()
                .email("john@example.com")
                .passwordHash("hash")
                .build();
        user.setId(1L);

        when(userRepository.findByEmailWithRoles("john@example.com"))
                .thenReturn(Optional.of(user));

        when(jwtUtils.generateToken(any(Authentication.class))).thenReturn("access-token");

        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse response = authService.login(request);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
    }

    @Test
    void refreshToken_shouldRotateToken() {
        String tokenId = UUID.randomUUID().toString();
        String rawToken = "raw-refresh-token";

        User user = User.builder().email("john@example.com").build();
        user.setId(1L);

        RefreshToken storedToken = RefreshToken.builder()
                .tokenId(tokenId)
                .user(user)
                .tokenHash("encodedHash")
                .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByTokenId(tokenId)).thenReturn(Optional.of(storedToken));
        when(passwordEncoder.matches(rawToken, "encodedHash")).thenReturn(true);

        UserRole userRole = UserRole.builder()
                .user(user)
                .role(Role.builder().name(RoleName.ROLE_USER).build())
                .build();

        when(userRoleRepository.findByUser(any(User.class))).thenReturn(List.of(userRole));
        when(jwtUtils.generateToken(any(Authentication.class))).thenReturn("new-access-token");
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse response = authService.refreshToken(rawToken, tokenId);

        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(storedToken.isRevoked()).isTrue();
    }

    @Test
    void refreshToken_shouldThrow_whenExpired() {
        String tokenId = UUID.randomUUID().toString();

        RefreshToken expiredToken = RefreshToken.builder()
                .tokenId(tokenId)
                .tokenHash("hash")
                .expiresAt(Instant.now().minus(1, ChronoUnit.HOURS))
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByTokenId(tokenId)).thenReturn(Optional.of(expiredToken));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        assertThatThrownBy(() -> authService.refreshToken("token", tokenId))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Refresh token expired or revoked");
    }

    @Test
    void logout_shouldRevokeToken() {
        String tokenId = UUID.randomUUID().toString();

        RefreshToken token = RefreshToken.builder()
                .tokenId(tokenId)
                .tokenHash("hash")
                .expiresAt(Instant.now().plus(1, ChronoUnit.DAYS))
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByTokenId(tokenId)).thenReturn(Optional.of(token));

        authService.logout(tokenId);

        assertThat(token.isRevoked()).isTrue();
        assertThat(token.getRevokedAt()).isNotNull();
    }

    @Test
    void logout_shouldThrow_whenTokenNotFound() {
        when(refreshTokenRepository.findByTokenId("invalid-id"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.logout("invalid-id"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
