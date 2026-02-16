package com.devoops.user.service;

import com.devoops.user.dto.request.ChangePasswordRequest;
import com.devoops.user.dto.request.UpdateUserRequest;
import com.devoops.user.dto.response.AuthenticationResponse;
import com.devoops.user.dto.response.UserResponse;
import com.devoops.user.entity.User;
import com.devoops.user.exception.InvalidPasswordException;
import com.devoops.user.exception.UserAlreadyExistsException;
import com.devoops.user.exception.UserNotFoundException;
import com.devoops.user.mapper.UserMapper;
import com.devoops.user.repository.UserRepository;
import com.devoops.user.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserResponse getProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User does not exist"));
        return userMapper.toUserResponse(user);
    }

    public AuthenticationResponse updateProfile(UUID userId, UpdateUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User does not exist"));

        if (request.username() != null && !request.username().equals(user.getUsername())) {
            if (userRepository.existsByUsername(request.username())) {
                throw new UserAlreadyExistsException("Username already taken");
            }
            user.setUsername(request.username());
        }

        if (request.email() != null && !request.email().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.email())) {
                throw new UserAlreadyExistsException("Email already taken");
            }
            user.setEmail(request.email());
        }

        if (request.firstName() != null) user.setFirstName(request.firstName());
        if (request.lastName() != null) user.setLastName(request.lastName());
        if (request.residence() != null) user.setResidence(request.residence());

        User saved = userRepository.save(user);
        return new AuthenticationResponse(jwtService.generateToken(saved), jwtService.getExpirationTime(), userMapper.toUserResponse(saved));
    }

    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User does not exist"));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new InvalidPasswordException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }
}
