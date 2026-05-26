package com.versus.api.admin.dto;

import java.time.Instant;
import java.util.UUID;

public record AdminUserResponse(
        UUID id,
        String username,
        String email,
        String avatarUrl,
        String role,
        boolean isActive,
        Instant createdAt
) {}
