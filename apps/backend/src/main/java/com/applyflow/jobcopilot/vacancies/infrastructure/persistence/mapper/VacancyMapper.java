package com.applyflow.jobcopilot.vacancies.infrastructure.persistence.mapper;

import com.applyflow.jobcopilot.vacancies.domain.Vacancy;
import com.applyflow.jobcopilot.vacancies.domain.VacancyStatus;
import com.applyflow.jobcopilot.vacancies.infrastructure.persistence.entity.VacancyJpaEntity;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class VacancyMapper {
    public Vacancy toDomain(VacancyJpaEntity entity) {
        List<String> skills = entity.getRequiredSkills() == null || entity.getRequiredSkills().isBlank()
                ? List.of()
                : Arrays.stream(entity.getRequiredSkills().split(",")).map(String::trim).filter(s -> !s.isBlank()).toList();
        return new Vacancy(entity.getId(), entity.getCreatedAt(), entity.getUpdatedAt(), entity.getPlatformId(), entity.getExternalId(), entity.getTitle(), entity.getCompany(), entity.getLocation(), entity.isRemote(), entity.getSeniority(), VacancyStatus.valueOf(entity.getStatus()), skills);
    }
}