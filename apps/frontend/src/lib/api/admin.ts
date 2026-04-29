import { apiRequest } from "@/lib/api/client";
import type { AdminIngestionOverviewResponse } from "@/types/api";

export const adminApi = {
  ingestionOverview: (token: string) =>
    apiRequest<AdminIngestionOverviewResponse>("/api/v1/admin/ingestion/overview", {
      token,
    }),
};
