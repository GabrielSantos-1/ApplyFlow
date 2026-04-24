import { apiRequest } from "@/lib/api/client";
import type {
  CreateResumeMetadataRequest,
  CreateResumeVariantRequest,
  PageResponse,
  ResumeResponse,
  ResumeVariantResponse,
  UploadResumePdfRequest,
} from "@/types/api";

export const resumesApi = {
  list: (token: string, page = 0, size = 20) =>
    apiRequest<PageResponse<ResumeResponse>>(`/api/v1/resumes?page=${page}&size=${size}`, {
      token,
    }),
  create: (token: string, payload: CreateResumeMetadataRequest) =>
    apiRequest<ResumeResponse>("/api/v1/resumes", {
      method: "POST",
      token,
      body: payload,
    }),
  byId: (token: string, resumeId: string) => apiRequest<ResumeResponse>(`/api/v1/resumes/${resumeId}`, { token }),
  createVariant: (token: string, resumeId: string, payload: CreateResumeVariantRequest) =>
    apiRequest<ResumeVariantResponse>(`/api/v1/resumes/${resumeId}/variants`, {
      method: "POST",
      token,
      body: payload,
    }),
  uploadPdf: (token: string, payload: UploadResumePdfRequest) => {
    const form = new FormData();
    form.set("title", payload.title);
    form.set("file", payload.file);
    return apiRequest<ResumeResponse>("/api/v1/resumes", {
      method: "POST",
      token,
      body: form,
    });
  },
};
