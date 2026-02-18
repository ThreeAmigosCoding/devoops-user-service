package com.devoops.user.controller;

import com.devoops.user.config.RequireRole;
import com.devoops.user.dto.response.UserResponse;
import com.devoops.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserPublicController {

    private final UserService userService;

    @GetMapping("/{id}")
    @RequireRole({"HOST", "GUEST"})
    public ResponseEntity<UserResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(userService.getProfile(UUID.fromString(id)));
    }
}
