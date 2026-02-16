package com.devoops.user.service;

import com.devoops.user.dto.request.LoginRequest;
import com.devoops.user.dto.request.RegisterRequest;
import com.devoops.user.dto.response.AuthenticationResponse;
import com.devoops.user.entity.User;
import com.devoops.user.exception.InvalidCredentialsException;
import com.devoops.user.exception.UserAlreadyExistsException;
import com.devoops.user.mapper.UserMapper;
import com.devoops.user.repository.UserRepository;
import com.devoops.user.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserMapper userMapper;
    private final UserEventPublisherService userEventPublisherService;

    @Transactional
    public AuthenticationResponse register(RegisterRequest request) {
        log.info("Registration attempt for username: {}, email: {}", request.username(), request.email());

        if (userRepository.existsByUsername(request.username())) {
            log.warn("Registration failed - username already exists: {}", request.username());
            throw new UserAlreadyExistsException("Username already taken: " + request.username());
        }

        if (userRepository.existsByEmail(request.email())) {
            log.warn("Registration failed - email already exists: {}", request.email());
            throw new UserAlreadyExistsException("Email already registered: " + request.email());
        }

        User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.password()));

        User savedUser = userRepository.save(user);
        String token = jwtService.generateToken(savedUser);

        log.info("Registration successful for user: {} (id: {}, role: {})",
                savedUser.getUsername(), savedUser.getId(), savedUser.getRole());

        userEventPublisherService.publishUserCreated(savedUser.getId(), savedUser.getEmail());

        return new AuthenticationResponse(token, jwtService.getExpirationTime(), userMapper.toUserResponse(savedUser));
    }

    public AuthenticationResponse login(LoginRequest request) {
        log.info("Login attempt for: {}", request.usernameOrEmail());

        User user = userRepository.findByUsernameOrEmail(request.usernameOrEmail(), request.usernameOrEmail())
            .orElseThrow(() -> {
                log.warn("Login failed - user not found: {}", request.usernameOrEmail());
                return new InvalidCredentialsException("Invalid username/email or password");
            });

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            log.warn("Login failed - invalid password for user: {}", user.getUsername());
            throw new InvalidCredentialsException("Invalid username/email or password");
        }

        String token = jwtService.generateToken(user);

        log.info("Login successful for user: {} (id: {}, role: {})",
                user.getUsername(), user.getId(), user.getRole());

        return new AuthenticationResponse(token, jwtService.getExpirationTime(), userMapper.toUserResponse(user));
    }
}
