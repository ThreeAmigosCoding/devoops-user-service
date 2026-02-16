package com.devoops.user.config;

import java.util.UUID;

public record UserContext(UUID userId, String role) { }
