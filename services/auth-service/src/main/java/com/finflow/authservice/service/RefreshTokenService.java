package com.finflow.authservice.service;

import com.finflow.authservice.entity.RefreshToken;
import com.finflow.authservice.entity.User;
import com.finflow.authservice.exception.InvalidRefreshTokenException;
import com.finflow.authservice.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    public RefreshToken createRefreshToken(User user) {

        RefreshToken refreshToken = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiryDate(
                        Instant.now().plusMillis(refreshExpiration)
                )
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    public RefreshToken verifyExpiration(RefreshToken token) {

        if (token.getExpiryDate().isBefore(Instant.now())) {

            refreshTokenRepository.delete(token);

            throw new RuntimeException("Refresh token expired");
        }

        return token;
    }

    public RefreshToken findByToken(String token) {
        return refreshTokenRepository.findByToken(token)
                .orElseThrow(() ->
                        new InvalidRefreshTokenException("Invalid refresh token"));
    }
}