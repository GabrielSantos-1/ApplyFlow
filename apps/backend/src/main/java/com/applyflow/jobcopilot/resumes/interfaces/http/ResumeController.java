package com.applyflow.jobcopilot.resumes.interfaces.http;

import com.applyflow.jobcopilot.resumes.application.dto.request.CreateResumeMetadataRequest;
import com.applyflow.jobcopilot.resumes.application.dto.request.GenerateResumeVariantRequest;
import com.applyflow.jobcopilot.resumes.application.dto.request.UploadResumePdfCommand;
import com.applyflow.jobcopilot.resumes.application.dto.response.ResumeResponse;
import com.applyflow.jobcopilot.resumes.application.dto.response.ResumeVariantResponse;
import com.applyflow.jobcopilot.resumes.application.usecase.ResumeUseCase;
import com.applyflow.jobcopilot.shared.application.exception.BadRequestException;
import com.applyflow.jobcopilot.shared.application.dto.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/resumes")
@Validated
public class ResumeController {
    private final ResumeUseCase resumeUseCase;

    public ResumeController(ResumeUseCase resumeUseCase) {
        this.resumeUseCase = resumeUseCase;
    }

    @GetMapping
    public ResponseEntity<PageResponse<ResumeResponse>> list(@RequestParam(defaultValue = "0") @Min(0) int page,
                                                             @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(resumeUseCase.list(page, size));
    }

    @PostMapping
    public ResponseEntity<ResumeResponse> create(@Valid @RequestBody CreateResumeMetadataRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(resumeUseCase.create(request));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResumeResponse> uploadPdf(@RequestParam("file") MultipartFile file,
                                                    @RequestParam("title") String title) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Arquivo PDF obrigatorio");
        }
        try {
            UploadResumePdfCommand command = new UploadResumePdfCommand(
                    title,
                    file.getOriginalFilename(),
                    file.getContentType(),
                    file.getBytes()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(resumeUseCase.uploadPdf(command));
        } catch (java.io.IOException ex) {
            throw new BadRequestException("Falha ao ler arquivo enviado");
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResumeResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(resumeUseCase.getById(id));
    }

    @PostMapping("/{id}/variants")
    public ResponseEntity<ResumeVariantResponse> createVariant(@PathVariable UUID id,
                                                               @Valid @RequestBody GenerateResumeVariantRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(resumeUseCase.createVariant(id, request));
    }
}
