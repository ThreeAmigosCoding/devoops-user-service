package com.devoops.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
    @Size(min = 3, max = 50) String username,
    @Email String email,
    @Size(max = 100) String firstName,
    @Size(max = 100) String lastName,
    @Size(max = 150) String residence
) {}
