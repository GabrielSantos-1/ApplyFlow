package com.applyflow.jobcopilot.ai.application.port;

import com.applyflow.jobcopilot.ai.domain.AiFlow;

public interface AiTextGenerationPort {
    AiTextGenerationResult generate(AiFlow flow, String systemPrompt, String userPrompt);

    String providerName();

    record AiTextGenerationResult(
            String content,
            String model,
            Integer promptTokens,
            Integer completionTokens,
            Integer totalTokens
    ) {
    }
}
