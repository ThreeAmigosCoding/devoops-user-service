package com.devoops.user.controller;

import com.devoops.user.dto.request.ChangePasswordRequest;
import com.devoops.user.dto.request.UpdateUserRequest;
import com.devoops.user.dto.response.AuthenticationResponse;
import com.devoops.user.dto.response.UserResponse;
import com.devoops.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user/me")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasAnyRole('HOST', 'GUEST')")
    public ResponseEntity<UserResponse> getProfile(Authentication auth) {
        return ResponseEntity.ok(userService.getProfile(auth));
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('HOST', 'GUEST')")
    public ResponseEntity<AuthenticationResponse> updateProfile(
            Authentication auth,
            @RequestBody @Valid UpdateUserRequest request) {
        return ResponseEntity.ok(userService.updateProfile(auth, request));
    }

    @PutMapping("/password")
    @PreAuthorize("hasAnyRole('HOST', 'GUEST')")
    public ResponseEntity<Void> changePassword(
            Authentication auth,
            @RequestBody @Valid ChangePasswordRequest request) {
        userService.changePassword(auth, request);
        return ResponseEntity.noContent().build();
    }
}
