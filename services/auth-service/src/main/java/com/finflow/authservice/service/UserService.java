package com.finflow.authservice.service;

import com.finflow.authservice.dto.LoginRequest;
import com.finflow.authservice.dto.RegisterRequest;
import com.finflow.authservice.entity.User;
import com.finflow.authservice.repository.UserRepository;
import com.finflow.authservice.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.finflow.authservice.entity.Role;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public User register(RegisterRequest request){
        if(userRepository.existsByEmail(request.getEmail())){
            throw new RuntimeException("Email already exists");
        }

        User user=User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(Role.CUSTOMER)
                .build();

        return userRepository.save(user);
    }
    public String login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail()).orElseThrow(() -> new RuntimeException("nvalid email or password"));

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPasswordHash())) {

            throw new RuntimeException(("Invalid email or password"));
        }
        return jwtService.generateToken(user.getEmail());
    }
}
