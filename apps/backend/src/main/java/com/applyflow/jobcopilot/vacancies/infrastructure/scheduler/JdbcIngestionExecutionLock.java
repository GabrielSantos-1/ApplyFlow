package com.applyflow.jobcopilot.vacancies.infrastructure.scheduler;

import com.applyflow.jobcopilot.vacancies.application.ingestion.contract.VacancyIngestionSource;
import com.applyflow.jobcopilot.vacancies.application.ingestion.port.IngestionExecutionLock;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class JdbcIngestionExecutionLock implements IngestionExecutionLock {
    private final JdbcTemplate jdbcTemplate;

    public JdbcIngestionExecutionLock(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean tryAcquire(VacancyIngestionSource source) {
        try {
            jdbcTemplate.update("insert into vacancy_ingestion_locks(source, locked_at) values (?, now())", source.name());
            return true;
        } catch (DataIntegrityViolationException ex) {
            return false;
        }
    }

    @Override
    public void release(VacancyIngestionSource source) {
        jdbcTemplate.update("delete from vacancy_ingestion_locks where source = ?", source.name());
    }
}
