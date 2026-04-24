package com.applyflow.jobcopilot.applications.infrastructure.persistence.mapper;

import com.applyflow.jobcopilot.applications.domain.ApplicationDraft;
import com.applyflow.jobcopilot.applications.domain.ApplicationStatus;
import com.applyflow.jobcopilot.applications.infrastructure.persistence.entity.ApplicationDraftJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class ApplicationDraftMapper {
    public ApplicationDraft toDomain(ApplicationDraftJpaEntity entity) {
        return new ApplicationDraft(entity.getId(), entity.getCreatedAt(), entity.getUpdatedAt(), entity.getUserId(), entity.getVacancyId(), entity.getResumeVariantId(), entity.getMessageDraft(), ApplicationStatus.valueOf(entity.getStatus()));
    }
}