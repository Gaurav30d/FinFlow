package com.finflow.authservice.service;

import com.finflow.authservice.dto.AuthResult;
import com.finflow.authservice.dto.LoginRequest;
import com.finflow.authservice.dto.RegisterRequest;
import com.finflow.authservice.entity.RefreshToken;
import com.finflow.authservice.entity.User;
import com.finflow.authservice.repository.UserRepository;
import com.finflow.authservice.security.JwtService;
import com.finflow.authservice.event.AuthEventProducer;
import com.finflow.authservice.event.AuthSecurityEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.finflow.authservice.entity.Role;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AuthEventProducer authEventProducer;

    public User register(RegisterRequest request){
        if(userRepository.existsByEmail(request.getEmail())){
            publishSecurityEvent("USER_REGISTER_FAILURE", null, "USER", null, "FAILURE");
            throw new RuntimeException("Email already exists");
        }

        User user=User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(Role.CUSTOMER)
                .build();

        User savedUser = userRepository.save(user);

        publishSecurityEvent("USER_REGISTER_SUCCESS", savedUser.getId(), "USER", String.valueOf(savedUser.getId()), "SUCCESS");

        return savedUser;
    }

    public AuthResult login(LoginRequest request) {
        java.util.Optional<User> userOptional = userRepository.findByEmail(request.getEmail());
        if (userOptional.isEmpty()) {
            publishSecurityEvent("USER_LOGIN_FAILURE", null, "USER", null, "FAILURE");
            throw new RuntimeException("Invalid email or password");
        }

        User user = userOptional.get();

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            publishSecurityEvent("USER_LOGIN_FAILURE", null, "USER", null, "FAILURE");
            throw new RuntimeException("Invalid email or password");
        }

        String accessToken = jwtService.generateToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        publishSecurityEvent("USER_LOGIN_SUCCESS", user.getId(), "USER", String.valueOf(user.getId()), "SUCCESS");

        return AuthResult.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .build();
    }

    private void publishSecurityEvent(String eventType, Long actorUserId, String resourceType, String resourceId, String outcome) {
        try {
            AuthSecurityEvent event = AuthSecurityEvent.builder()
                    .eventId(UUID.randomUUID())
                    .eventType(eventType)
                    .actorUserId(actorUserId)
                    .resourceType(resourceType)
                    .resourceId(resourceId)
                    .occurredAt(Instant.now())
                    .outcome(outcome)
                    .correlationId(null)
                    .build();

            authEventProducer.publishEvent(event);
        } catch (Exception ex) {
            log.error("Failed to publish {} audit event: {}", eventType, ex.getMessage());
        }
    }
}
