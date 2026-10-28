package com.finflow.authservice.service;

import com.finflow.authservice.dto.AuthResult;
import com.finflow.authservice.dto.LoginRequest;
import com.finflow.authservice.dto.RegisterRequest;
import com.finflow.authservice.entity.RefreshToken;
import com.finflow.authservice.entity.Role;
import com.finflow.authservice.entity.User;
import com.finflow.authservice.event.AuthEventProducer;
import com.finflow.authservice.event.AuthSecurityEvent;
import com.finflow.authservice.repository.UserRepository;
import com.finflow.authservice.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private AuthEventProducer authEventProducer;

    @Captor
    private ArgumentCaptor<AuthSecurityEvent> eventCaptor;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(
                userRepository,
                passwordEncoder,
                jwtService,
                refreshTokenService,
                authEventProducer
        );
    }

    @Test
    @DisplayName("register publishes USER_REGISTER_SUCCESS with correct audit fields after user persistence")
    void register_Success_PublishesUserRegisterSuccessEventWithCorrectFields() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("alice@example.com");
        request.setPassword("SecurePassword123!");

        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("SecurePassword123!")).thenReturn("encodedHash123");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            return User.builder()
                    .id(101L)
                    .email(user.getEmail())
                    .passwordHash(user.getPasswordHash())
                    .role(Role.CUSTOMER)
                    .build();
        });

        Instant beforeRegister = Instant.now().minus(1, ChronoUnit.SECONDS);
        User registered = userService.register(request);

        assertThat(registered).isNotNull();
        assertThat(registered.getId()).isEqualTo(101L);

        verify(authEventProducer).publishEvent(eventCaptor.capture());
        AuthSecurityEvent event = eventCaptor.getValue();

        assertThat(event).isNotNull();
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getEventType()).isEqualTo("USER_REGISTER_SUCCESS");
        assertThat(event.getActorUserId()).isEqualTo(101L);
        assertThat(event.getResourceType()).isEqualTo("USER");
        assertThat(event.getResourceId()).isEqualTo("101");
        assertThat(event.getOccurredAt()).isAfterOrEqualTo(beforeRegister);
        assertThat(event.getOccurredAt()).isBeforeOrEqualTo(Instant.now().plus(1, ChronoUnit.SECONDS));
        assertThat(event.getOutcome()).isEqualTo("SUCCESS");
        assertThat(event.getCorrelationId()).isNull();
        assertThat(event.getServiceName()).isEqualTo("auth-service");
    }

    @Test
    @DisplayName("register publishes USER_REGISTER_FAILURE when email already exists")
    void register_EmailAlreadyExists_PublishesUserRegisterFailureAndThrows() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("existing@example.com");
        request.setPassword("Password123!");

        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Email already exists");

        verify(userRepository, never()).save(any());
        verify(authEventProducer).publishEvent(eventCaptor.capture());
        AuthSecurityEvent event = eventCaptor.getValue();

        assertThat(event).isNotNull();
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getEventType()).isEqualTo("USER_REGISTER_FAILURE");
        assertThat(event.getActorUserId()).isNull();
        assertThat(event.getResourceType()).isEqualTo("USER");
        assertThat(event.getResourceId()).isNull();
        assertThat(event.getOutcome()).isEqualTo("FAILURE");
        assertThat(event.getCorrelationId()).isNull();
        assertThat(event.getServiceName()).isEqualTo("auth-service");
    }

    @Test
    @DisplayName("register does not publish event if database save fails")
    void register_DatabaseSaveFails_DoesNotPublishEvent() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("bob@example.com");
        request.setPassword("Password123!");

        when(userRepository.existsByEmail("bob@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("encodedHash");
        when(userRepository.save(any(User.class))).thenThrow(new RuntimeException("Database connection timed out"));

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Database connection timed out");

        verify(authEventProducer, never()).publishEvent(any());
    }

    @Test
    @DisplayName("register preserves user creation success even if audit publishing throws an unexpected exception")
    void register_PublishThrowsException_PreservesRegistrationSuccess() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("carol@example.com");
        request.setPassword("Password123!");

        when(userRepository.existsByEmail("carol@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("encodedHash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            return User.builder()
                    .id(202L)
                    .email(user.getEmail())
                    .passwordHash(user.getPasswordHash())
                    .role(Role.CUSTOMER)
                    .build();
        });
        doThrow(new RuntimeException("Kafka producer buffer unavailable")).when(authEventProducer).publishEvent(any());

        assertThatCode(() -> {
            User user = userService.register(request);
            assertThat(user).isNotNull();
            assertThat(user.getId()).isEqualTo(202L);
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("login publishes USER_LOGIN_SUCCESS with correct audit fields upon successful authentication")
    void login_Success_PublishesUserLoginSuccessWithCorrectFields() {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@example.com");
        request.setPassword("Secret123!");

        User user = User.builder()
                .id(501L)
                .email("user@example.com")
                .passwordHash("hashedSecret")
                .role(Role.CUSTOMER)
                .build();

        RefreshToken refreshToken = RefreshToken.builder()
                .token("refresh-token-uuid")
                .user(user)
                .build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Secret123!", "hashedSecret")).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("access-token-jwt");
        when(refreshTokenService.createRefreshToken(user)).thenReturn(refreshToken);

        Instant beforeLogin = Instant.now().minus(1, ChronoUnit.SECONDS);
        AuthResult result = userService.login(request);

        assertThat(result).isNotNull();
        assertThat(result.getAccessToken()).isEqualTo("access-token-jwt");
        assertThat(result.getRefreshToken()).isEqualTo("refresh-token-uuid");

        verify(authEventProducer).publishEvent(eventCaptor.capture());
        AuthSecurityEvent event = eventCaptor.getValue();

        assertThat(event).isNotNull();
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getEventType()).isEqualTo("USER_LOGIN_SUCCESS");
        assertThat(event.getActorUserId()).isEqualTo(501L);
        assertThat(event.getResourceType()).isEqualTo("USER");
        assertThat(event.getResourceId()).isEqualTo("501");
        assertThat(event.getOccurredAt()).isAfterOrEqualTo(beforeLogin);
        assertThat(event.getOccurredAt()).isBeforeOrEqualTo(Instant.now().plus(1, ChronoUnit.SECONDS));
        assertThat(event.getOutcome()).isEqualTo("SUCCESS");
        assertThat(event.getCorrelationId()).isNull();
        assertThat(event.getServiceName()).isEqualTo("auth-service");
    }

    @Test
    @DisplayName("login publishes USER_LOGIN_FAILURE when email is not found")
    void login_UnknownEmail_PublishesUserLoginFailureAndThrows() {
        LoginRequest request = new LoginRequest();
        request.setEmail("unknown@example.com");
        request.setPassword("Secret123!");

        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Invalid email or password");

        verify(authEventProducer).publishEvent(eventCaptor.capture());
        AuthSecurityEvent event = eventCaptor.getValue();

        assertThat(event).isNotNull();
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getEventType()).isEqualTo("USER_LOGIN_FAILURE");
        assertThat(event.getActorUserId()).isNull();
        assertThat(event.getResourceType()).isEqualTo("USER");
        assertThat(event.getResourceId()).isNull();
        assertThat(event.getOutcome()).isEqualTo("FAILURE");
        assertThat(event.getCorrelationId()).isNull();
        assertThat(event.getServiceName()).isEqualTo("auth-service");
    }

    @Test
    @DisplayName("login publishes USER_LOGIN_FAILURE when password does not match")
    void login_IncorrectPassword_PublishesUserLoginFailureAndThrows() {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@example.com");
        request.setPassword("WrongPassword!");

        User user = User.builder()
                .id(502L)
                .email("user@example.com")
                .passwordHash("hashedSecret")
                .role(Role.CUSTOMER)
                .build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPassword!", "hashedSecret")).thenReturn(false);

        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Invalid email or password");

        verify(authEventProducer).publishEvent(eventCaptor.capture());
        AuthSecurityEvent event = eventCaptor.getValue();

        assertThat(event).isNotNull();
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getEventType()).isEqualTo("USER_LOGIN_FAILURE");
        assertThat(event.getActorUserId()).isNull();
        assertThat(event.getResourceType()).isEqualTo("USER");
        assertThat(event.getResourceId()).isNull();
        assertThat(event.getOutcome()).isEqualTo("FAILURE");
        assertThat(event.getCorrelationId()).isNull();
        assertThat(event.getServiceName()).isEqualTo("auth-service");
    }

    @Test
    @DisplayName("login preserves authentication success even if audit publishing throws an unexpected exception")
    void login_PublishThrowsException_PreservesLoginSuccess() {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@example.com");
        request.setPassword("Secret123!");

        User user = User.builder()
                .id(503L)
                .email("user@example.com")
                .passwordHash("hashedSecret")
                .role(Role.CUSTOMER)
                .build();

        RefreshToken refreshToken = RefreshToken.builder()
                .token("refresh-token-uuid")
                .user(user)
                .build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Secret123!", "hashedSecret")).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("access-token-jwt");
        when(refreshTokenService.createRefreshToken(user)).thenReturn(refreshToken);
        doThrow(new RuntimeException("Kafka unreachable")).when(authEventProducer).publishEvent(any());

        assertThatCode(() -> {
            AuthResult result = userService.login(request);
            assertThat(result).isNotNull();
            assertThat(result.getAccessToken()).isEqualTo("access-token-jwt");
            assertThat(result.getRefreshToken()).isEqualTo("refresh-token-uuid");
        }).doesNotThrowAnyException();
    }
}
