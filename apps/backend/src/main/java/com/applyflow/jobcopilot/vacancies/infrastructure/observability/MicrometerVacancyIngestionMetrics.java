package com.applyflow.jobcopilot.vacancies.infrastructure.observability;

import com.applyflow.jobcopilot.vacancies.application.ingestion.contract.VacancyIngestionSource;
import com.applyflow.jobcopilot.vacancies.application.ingestion.port.VacancyIngestionMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class MicrometerVacancyIngestionMetrics implements VacancyIngestionMetrics {
    private static final Logger log = LoggerFactory.getLogger(MicrometerVacancyIngestionMetrics.class);

    private final MeterRegistry meterRegistry;

    public MicrometerVacancyIngestionMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void recordRunStarted(VacancyIngestionSource source, String triggerType) {
        safeRecord("run_started", () -> {
            meterRegistry.counter("applyflow_ingestion_total",
                    "source", source.name().toLowerCase()).increment();
            meterRegistry.counter("applyflow_ingestion_runs_started_total",
                "source", source.name().toLowerCase(),
                "trigger", safeTag(triggerType)).increment();
        });
    }

    @Override
    public void recordRunCompleted(VacancyIngestionSource source, String status, Duration duration) {
        safeRecord("run_completed", () -> {
            meterRegistry.counter("applyflow_ingestion_runs_completed_total",
                    "source", source.name().toLowerCase(),
                    "status", safeTag(status)).increment();
            if ("FAILED".equalsIgnoreCase(status)) {
                meterRegistry.counter("applyflow_ingestion_failed",
                        "source", source.name().toLowerCase()).increment();
            } else {
                meterRegistry.counter("applyflow_ingestion_success",
                        "source", source.name().toLowerCase()).increment();
            }
            Timer.builder("applyflow_ingestion_run_duration_seconds")
                    .tag("source", source.name().toLowerCase())
                    .publishPercentiles(0.5, 0.95, 0.99)
                    .register(meterRegistry)
                    .record(duration);
        });
    }

    @Override
    public void recordStageFailure(VacancyIngestionSource source, String stage) {
        safeRecord("stage_failure", () -> meterRegistry.counter("applyflow_ingestion_stage_failures_total",
                "source", source.name().toLowerCase(),
                "stage", safeTag(stage)).increment());
    }

    @Override
    public void recordSkipped(VacancyIngestionSource source) {
        safeRecord("skipped", () -> meterRegistry.counter("applyflow_ingestion_skipped_total",
                "source", source.name().toLowerCase()).increment());
    }

    @Override
    public void recordInserted(VacancyIngestionSource source) {
        safeRecord("inserted", () -> meterRegistry.counter("applyflow_ingestion_inserted_total",
                "source", source.name().toLowerCase()).increment());
    }

    @Override
    public void recordUpdated(VacancyIngestionSource source) {
        safeRecord("updated", () -> meterRegistry.counter("applyflow_ingestion_updated_total",
                "source", source.name().toLowerCase()).increment());
    }

    private void safeRecord(String signal, Runnable action) {
        try {
            action.run();
        } catch (Exception ex) {
            log.warn("eventType=observability.metric_emission_failed severity=WARN signal=vacancy_ingestion_{} reason={}",
                    signal, ex.toString());
        }
    }

    private String safeTag(String raw) {
        if (raw == null || raw.isBlank()) {
            return "unknown";
        }
        String cleaned = raw.toLowerCase().replaceAll("[^a-z0-9_.:-]", "_");
        return cleaned.length() > 40 ? cleaned.substring(0, 40) : cleaned;
    }
}
