package com.finflow.authservice.controller;

import com.finflow.authservice.dto.*;
import com.finflow.authservice.entity.RefreshToken;
import com.finflow.authservice.entity.User;
import com.finflow.authservice.security.JwtService;
import com.finflow.authservice.service.RefreshTokenService;
import com.finflow.authservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(
            @Valid @RequestBody RegisterRequest request) {

        User user = userService.register(request);

        UserResponse response = UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .build();

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResult result = userService.login(request);

        LoginResponse response= LoginResponse.builder()
                .accessToken(result.getAccessToken())
                .refreshToken(result.getRefreshToken())
                .tokenType("Bearer")
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<String> me() {
        return ResponseEntity.ok("Authenticated successfully");
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(
            @RequestBody RefreshTokenRequest request) {

        RefreshToken refreshToken =
                refreshTokenService.findByToken(
                        request.getRefreshToken()
                );

        refreshToken = refreshTokenService.verifyExpiration(refreshToken);

        String accessToken =
                jwtService.generateToken(
                        refreshToken.getUser().getEmail()
                );

        LoginResponse response = LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .build();

        return ResponseEntity.ok(response);
    }

}