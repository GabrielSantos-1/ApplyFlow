import { apiRequest } from "@/lib/api/client";
import type { PageResponse, VacancyDetail, VacancyListItem } from "@/types/api";

export type VacancyListQuery = {
  page?: number;
  size?: number;
  sortBy?: "createdAt" | "title" | "company";
  sortDirection?: "asc" | "desc";
  query?: string;
  workModel?: "remote" | "hybrid" | "onsite" | "all";
  seniority?: "junior" | "pleno" | "senior" | "especialista" | "all";
};

function toQueryString(query: VacancyListQuery): string {
  const params = new URLSearchParams();
  Object.entries(query).forEach(([key, value]) => {
    if (value === undefined || value === null || value === "") {
      return;
    }
    params.set(key, String(value));
  });
  return params.toString();
}

export const vacanciesApi = {
  list: (token: string, query: VacancyListQuery = {}) => {
    const qs = toQueryString({ page: 0, size: 20, sortBy: "createdAt", sortDirection: "desc", ...query });
    return apiRequest<PageResponse<VacancyListItem>>(`/api/v1/vacancies?${qs}`, { token });
  },
  byId: (token: string, id: string) => apiRequest<VacancyDetail>(`/api/v1/vacancies/${id}`, { token }),
};

