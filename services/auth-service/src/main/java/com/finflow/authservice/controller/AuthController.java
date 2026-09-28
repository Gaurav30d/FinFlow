package com.finflow.authservice.controller;

import com.finflow.authservice.dto.LoginRequest;
import com.finflow.authservice.dto.LoginResponse;
import com.finflow.authservice.dto.RegisterRequest;
import com.finflow.authservice.dto.UserResponse;
import com.finflow.authservice.entity.User;
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
        String accessToken= userService.login(request);

        LoginResponse response= LoginResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<String> me() {
        return ResponseEntity.ok("Authenticated successfully");
    }
}