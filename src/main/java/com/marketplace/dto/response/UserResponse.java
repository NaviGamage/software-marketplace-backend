package com.marketplace.dto.response;

import com.marketplace.entity.User;
import com.marketplace.enums.UserRole;
import com.marketplace.enums.UserStatus;

import java.time.OffsetDateTime;

public record UserResponse(
        Long id,
        String email,
        String fullName,
        UserRole role,
        UserStatus status,
        OffsetDateTime createdAt
) {
    public static UserResponse fromEntity(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getStatus(),
                user.getCreatedAt()
        );
    }
}