import { apiRequest } from "@/lib/api/client";
import type { MatchAnalysisResponse } from "@/types/api";

export type MatchGenerateRequest = {
  vacancyId: string;
  resumeId?: string;
  resumeVariantId?: string;
  forceRegenerate?: boolean;
};

export const matchingApi = {
  byVacancy: (token: string, vacancyId: string) =>
    apiRequest<MatchAnalysisResponse>(`/api/v1/matches/vacancy/${vacancyId}`, { token }),
  generate: (token: string, request: MatchGenerateRequest) =>
    apiRequest<MatchAnalysisResponse>("/api/v1/matches", {
      token,
      method: "POST",
      body: request,
    }),
};
