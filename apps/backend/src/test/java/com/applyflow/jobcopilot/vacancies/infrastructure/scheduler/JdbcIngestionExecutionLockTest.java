package com.applyflow.jobcopilot.vacancies.infrastructure.scheduler;

import com.applyflow.jobcopilot.vacancies.application.ingestion.contract.VacancyIngestionSource;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class JdbcIngestionExecutionLockTest {

    @Test
    void shouldAcquireWhenInsertSucceeds() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        JdbcIngestionExecutionLock lock = new JdbcIngestionExecutionLock(jdbcTemplate);

        assertTrue(lock.tryAcquire(VacancyIngestionSource.REMOTIVE));
        verify(jdbcTemplate).update("insert into vacancy_ingestion_locks(source, locked_at) values (?, now())", "REMOTIVE");
    }

    @Test
    void shouldReturnFalseWhenLockAlreadyExists() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        doThrow(new DataIntegrityViolationException("duplicate key"))
                .when(jdbcTemplate).update("insert into vacancy_ingestion_locks(source, locked_at) values (?, now())", "REMOTIVE");
        JdbcIngestionExecutionLock lock = new JdbcIngestionExecutionLock(jdbcTemplate);

        assertFalse(lock.tryAcquire(VacancyIngestionSource.REMOTIVE));
    }
}
