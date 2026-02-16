package com.devoops.user.controller;

import com.devoops.user.config.RequireRole;
import com.devoops.user.config.UserContext;
import com.devoops.user.dto.request.ChangePasswordRequest;
import com.devoops.user.dto.request.UpdateUserRequest;
import com.devoops.user.dto.response.AuthenticationResponse;
import com.devoops.user.dto.response.UserResponse;
import com.devoops.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user/me")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @RequireRole({"HOST", "GUEST"})
    public ResponseEntity<UserResponse> getProfile(UserContext userContext) {
        return ResponseEntity.ok(userService.getProfile(userContext.userId()));
    }

    @PutMapping
    @RequireRole({"HOST", "GUEST"})
    public ResponseEntity<AuthenticationResponse> updateProfile(
            UserContext userContext,
            @RequestBody @Valid UpdateUserRequest request) {
        return ResponseEntity.ok(userService.updateProfile(userContext.userId(), request));
    }

    @PutMapping("/password")
    @RequireRole({"HOST", "GUEST"})
    public ResponseEntity<Void> changePassword(
            UserContext userContext,
            @RequestBody @Valid ChangePasswordRequest request) {
        userService.changePassword(userContext.userId(), request);
        return ResponseEntity.noContent().build();
    }
}
