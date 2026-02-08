package com.devoops.user.dto.response;

import com.devoops.user.entity.Role;

import java.util.UUID;

public record UserResponse(
    UUID id,
    String username,
    String email,
    String firstName,
    String lastName,
    String residence,
    Role role
) {}
