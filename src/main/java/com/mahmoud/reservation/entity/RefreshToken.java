package com.mahmoud.reservation.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Where;

import java.time.Instant;

@Entity
@Where(clause = "is_deleted = false")
@Table(
        name = "refresh_tokens",
        indexes = {
                @Index(name = "idx_refresh_token_user", columnList = "user_id"),
                @Index(name = "idx_refresh_token_token_id", columnList = "token_id"),
                @Index(name = "idx_refresh_token_revoked", columnList = "revoked")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_refresh_token_token_id", columnNames = "token_id")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken extends BaseEntity {

    @Column(name = "token_id", nullable = false, unique = true, length = 100)
    private String tokenId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token_hash", nullable = false)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked", nullable = false)
    private boolean revoked;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    public boolean isValid() {
        return !revoked && expiresAt.isAfter(Instant.now());
    }
}