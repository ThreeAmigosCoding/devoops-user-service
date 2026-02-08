package com.devoops.user.controller;

import com.devoops.user.dto.request.LoginRequest;
import com.devoops.user.dto.request.RegisterRequest;
import com.devoops.user.dto.response.AuthenticationResponse;
import com.devoops.user.service.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/user/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.debug("Received registration request for username: {}", request.username());

        AuthenticationResponse response = authenticationService.register(request);

        log.debug("Registration request completed.");
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(@Valid @RequestBody LoginRequest request) {
        log.debug("Received login request for: {}", request.usernameOrEmail());

        AuthenticationResponse response = authenticationService.login(request);

        log.debug("Login request completed.");
        return ResponseEntity.ok(response);
    }
}
