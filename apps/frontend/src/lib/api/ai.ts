import { apiRequest } from "@/lib/api/client";
import type {
  ApplicationDraftSuggestionResponse,
  CvImprovementResponse,
  MatchEnrichmentResponse,
} from "@/types/api";

export const aiApi = {
  enrichMatch: (token: string, vacancyId: string) =>
    apiRequest<MatchEnrichmentResponse>(`/api/v1/ai/matches/${vacancyId}/enrichment`, {
      method: "POST",
      token,
    }),
  improveCv: (token: string, vacancyId: string) =>
    apiRequest<CvImprovementResponse>(`/api/v1/ai/matches/${vacancyId}/cv-improvement`, {
      method: "POST",
      token,
    }),
  draftApplication: (token: string, vacancyId: string) =>
    apiRequest<ApplicationDraftSuggestionResponse>(`/api/v1/ai/matches/${vacancyId}/application-draft`, {
      method: "POST",
      token,
    }),
};

