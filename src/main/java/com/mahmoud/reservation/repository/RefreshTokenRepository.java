package com.mahmoud.reservation.repository;

import com.mahmoud.reservation.entity.RefreshToken;
import com.mahmoud.reservation.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenId(String tokenId);

    List<RefreshToken> findAllByUserAndRevokedFalse(User user);

    List<RefreshToken> findAllByUser(User user);
}