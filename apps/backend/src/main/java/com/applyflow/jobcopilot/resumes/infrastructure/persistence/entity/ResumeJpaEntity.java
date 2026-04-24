package com.applyflow.jobcopilot.resumes.infrastructure.persistence.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "resumes")
public class ResumeJpaEntity {
    @Id
    private UUID id;
    @Column(name = "user_id")
    private UUID userId;
    private String title;
    @Column(name = "source_file_name")
    private String sourceFileName;
    @Column(name = "storage_path")
    private String storagePath;
    @Column(name = "content_type")
    private String contentType;
    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;
    @Column(name = "file_checksum_sha256")
    private String fileChecksumSha256;
    @Column(name = "is_base")
    private boolean base;
    @Column(name = "uploaded_at")
    private OffsetDateTime uploadedAt;
    private String status;
    @Column(name = "created_at")
    private OffsetDateTime createdAt;
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSourceFileName() { return sourceFileName; }
    public void setSourceFileName(String sourceFileName) { this.sourceFileName = sourceFileName; }
    public String getStoragePath() { return storagePath; }
    public void setStoragePath(String storagePath) { this.storagePath = storagePath; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public Long getFileSizeBytes() { return fileSizeBytes; }
    public void setFileSizeBytes(Long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }
    public String getFileChecksumSha256() { return fileChecksumSha256; }
    public void setFileChecksumSha256(String fileChecksumSha256) { this.fileChecksumSha256 = fileChecksumSha256; }
    public boolean isBase() { return base; }
    public void setBase(boolean base) { this.base = base; }
    public OffsetDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(OffsetDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
