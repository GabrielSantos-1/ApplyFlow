package com.applyflow.jobcopilot.matching.application.dto;

import java.util.List;
import java.util.UUID;

public record MatchInput(
        UUID vacancyId,
        CandidateProfileData candidateProfile,
        ResumeData resumeData
) {
    public record CandidateProfileData(
            String headline,
            String summary,
            String location,
            List<String> primarySkills
    ) {
    }

    public record ResumeData(
            UUID resumeId,
            UUID resumeVariantId,
            String title,
            String sourceFileName,
            String variantLabel
    ) {
    }
}
