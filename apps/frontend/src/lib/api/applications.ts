import { apiRequest } from "@/lib/api/client";
import type {
  ApplicationDraftResponse,
  ApplicationTrackingEventResponse,
  ApplicationStatus,
  PageResponse,
} from "@/types/api";

export const applicationsApi = {
  list: (token: string, page = 0, size = 20) =>
    apiRequest<PageResponse<ApplicationDraftResponse>>(`/api/v1/applications?page=${page}&size=${size}`, {
      token,
    }),
  getById: (token: string, id: string) =>
    apiRequest<ApplicationDraftResponse>(`/api/v1/applications/${id}`, {
      token,
    }),
  updateStatus: (token: string, id: string, status: ApplicationStatus, notes?: string) =>
    apiRequest<ApplicationDraftResponse>(`/api/v1/applications/${id}/status`, {
      method: "PATCH",
      token,
      body: { status, notes: notes || undefined },
    }),
  createDraft: (token: string, payload: { vacancyId: string; resumeVariantId: string; messageDraft?: string }) =>
    apiRequest<ApplicationDraftResponse>("/api/v1/applications/drafts", {
      method: "POST",
      token,
      body: payload,
    }),
  createDraftAssisted: (token: string, payload: { vacancyId: string; resumeId?: string; messageDraft?: string }) =>
    apiRequest<ApplicationDraftResponse>("/api/v1/applications/drafts/assisted", {
      method: "POST",
      token,
      body: payload,
    }),
  getTracking: (token: string, id: string) =>
    apiRequest<ApplicationTrackingEventResponse[]>(`/api/v1/applications/${id}/tracking`, {
      token,
    }),
};

