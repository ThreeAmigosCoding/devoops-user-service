package com.devoops.user.dto.response;

public record AuthenticationResponse(
    String accessToken,
    String tokenType,
    long expiresIn,
    UserResponse user
) {
    public AuthenticationResponse(String accessToken, long expiresIn, UserResponse user) {
        this(accessToken, "Bearer", expiresIn, user);
    }
}
