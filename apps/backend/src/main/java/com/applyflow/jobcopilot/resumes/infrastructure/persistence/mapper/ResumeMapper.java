package com.applyflow.jobcopilot.resumes.infrastructure.persistence.mapper;

import com.applyflow.jobcopilot.resumes.domain.Resume;
import com.applyflow.jobcopilot.resumes.domain.ResumeStatus;
import com.applyflow.jobcopilot.resumes.infrastructure.persistence.entity.ResumeJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class ResumeMapper {
    public Resume toDomain(ResumeJpaEntity entity) {
        return new Resume(entity.getId(), entity.getCreatedAt(), entity.getUpdatedAt(), entity.getUserId(), entity.getTitle(), entity.getSourceFileName(), ResumeStatus.valueOf(entity.getStatus()));
    }
}