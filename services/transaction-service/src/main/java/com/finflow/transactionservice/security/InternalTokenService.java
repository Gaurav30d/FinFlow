package com.finflow.transactionservice.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class InternalTokenService {

    private final SecretKey secretKey;

    public InternalTokenService(
            @Value("${internal.jwt.secret}") String secret) {

        this.secretKey = Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );
    }

    public String generateToken() {

        Instant now = Instant.now();

        return Jwts.builder()
                .subject("transaction-service")
                .claim("service", true)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(60)))
                .signWith(secretKey)
                .compact();
    }
}