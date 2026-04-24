package com.applyflow.jobcopilot.resumes.infrastructure.storage;

import com.applyflow.jobcopilot.resumes.application.port.ResumeFileStoragePort;
import com.applyflow.jobcopilot.resumes.infrastructure.config.ResumeStorageProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

@Component
public class LocalResumeFileStorage implements ResumeFileStoragePort {
    private final ResumeStorageProperties properties;

    public LocalResumeFileStorage(ResumeStorageProperties properties) {
        this.properties = properties;
    }

    @Override
    public StoredResumeFile storePdf(UUID userId, UUID resumeId, byte[] content) throws IOException {
        Path basePath = Path.of(properties.getBaseDir()).toAbsolutePath().normalize();
        Path userDirectory = basePath.resolve(userId.toString()).normalize();
        if (!userDirectory.startsWith(basePath)) {
            throw new IOException("Diretorio de storage invalido");
        }
        Files.createDirectories(userDirectory);
        Path filePath = userDirectory.resolve(resumeId + ".pdf").normalize();
        if (!filePath.startsWith(basePath)) {
            throw new IOException("Path de storage invalido");
        }
        Files.write(filePath, content);
        return new StoredResumeFile(filePath.toString(), content.length, sha256(content));
    }

    private String sha256(byte[] content) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(content));
        } catch (NoSuchAlgorithmException ex) {
            throw new IOException("Algoritmo de checksum indisponivel", ex);
        }
    }
}
