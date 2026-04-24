package com.applyflow.jobcopilot.resumes.infrastructure.persistence.mapper;

import com.applyflow.jobcopilot.resumes.domain.ResumeVariant;
import com.applyflow.jobcopilot.resumes.domain.ResumeVariantStatus;
import com.applyflow.jobcopilot.resumes.infrastructure.persistence.entity.ResumeVariantJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class ResumeVariantMapper {
    public ResumeVariant toDomain(ResumeVariantJpaEntity entity) {
        return new ResumeVariant(entity.getId(), entity.getCreatedAt(), entity.getUpdatedAt(), entity.getResumeId(), entity.getVacancyId(), entity.getVariantLabel(), ResumeVariantStatus.valueOf(entity.getStatus()));
    }
}