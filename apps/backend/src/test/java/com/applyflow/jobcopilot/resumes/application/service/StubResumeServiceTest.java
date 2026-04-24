package com.applyflow.jobcopilot.resumes.application.service;

import com.applyflow.jobcopilot.auth.domain.UserRole;
import com.applyflow.jobcopilot.resumes.application.dto.request.UploadResumePdfCommand;
import com.applyflow.jobcopilot.resumes.application.port.ResumeFileStoragePort;
import com.applyflow.jobcopilot.resumes.infrastructure.config.ResumeStorageProperties;
import com.applyflow.jobcopilot.resumes.infrastructure.persistence.repository.ResumeJpaRepository;
import com.applyflow.jobcopilot.resumes.infrastructure.persistence.repository.ResumeVariantJpaRepository;
import com.applyflow.jobcopilot.shared.application.audit.AuditEventService;
import com.applyflow.jobcopilot.shared.application.exception.BadRequestException;
import com.applyflow.jobcopilot.shared.application.observability.OperationalEventLogger;
import com.applyflow.jobcopilot.shared.application.observability.OperationalMetricsService;
import com.applyflow.jobcopilot.shared.application.security.AuthContextService;
import com.applyflow.jobcopilot.shared.application.security.AuthenticatedUser;
import com.applyflow.jobcopilot.shared.application.security.TextSanitizer;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class StubResumeServiceTest {
    @Test
    void uploadPdfShouldRejectInvalidPdfSignature() {
        ResumeJpaRepository resumeRepo = mock(ResumeJpaRepository.class);
        ResumeVariantJpaRepository variantRepo = mock(ResumeVariantJpaRepository.class);
        ResumeFileStoragePort storage = mock(ResumeFileStoragePort.class);
        ResumeStorageProperties props = new ResumeStorageProperties();
        props.setMaxPdfBytes(1024);
        AuthContextService authCtx = mock(AuthContextService.class);
        when(authCtx.requireAuthenticatedUser()).thenReturn(new AuthenticatedUser(UUID.randomUUID(), "user@test.local", UserRole.USER));

        StubResumeService service = new StubResumeService(
                resumeRepo,
                variantRepo,
                storage,
                props,
                authCtx,
                new TextSanitizer(),
                mock(AuditEventService.class),
                mock(OperationalMetricsService.class),
                mock(OperationalEventLogger.class)
        );

        UploadResumePdfCommand command = new UploadResumePdfCommand(
                "Curriculo",
                "curriculo.pdf",
                "application/pdf",
                "not-a-pdf".getBytes()
        );

        assertThrows(BadRequestException.class, () -> service.uploadPdf(command));
        verifyNoInteractions(storage);
    }
}
