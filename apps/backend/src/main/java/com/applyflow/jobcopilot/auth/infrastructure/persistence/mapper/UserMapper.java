package com.applyflow.jobcopilot.auth.infrastructure.persistence.mapper;

import com.applyflow.jobcopilot.auth.domain.User;
import com.applyflow.jobcopilot.auth.domain.UserRole;
import com.applyflow.jobcopilot.auth.infrastructure.persistence.entity.UserJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {
    public User toDomain(UserJpaEntity entity) {
        return new User(entity.getId(), entity.getCreatedAt(), entity.getUpdatedAt(), entity.getEmail(), entity.getPasswordHash(), UserRole.valueOf(entity.getRole()), entity.isActive());
    }
}