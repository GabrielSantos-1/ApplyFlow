import { apiRequest } from "@/lib/api/client";
import type { AuthTokensResponse, CurrentUserResponse } from "@/types/api";

export type LoginPayload = {
  email: string;
  password: string;
};

export const authApi = {
  login: (payload: LoginPayload) =>
    apiRequest<AuthTokensResponse>("/api/v1/auth/login", {
      method: "POST",
      body: payload,
    }),
  me: (token: string) =>
    apiRequest<CurrentUserResponse>("/api/v1/auth/me", {
      token,
    }),
  refresh: (refreshToken?: string) =>
    apiRequest<AuthTokensResponse>("/api/v1/auth/refresh", {
      method: "POST",
      body: refreshToken ? { refreshToken } : {},
    }),
  logout: (refreshToken?: string, token?: string) =>
    apiRequest<void>("/api/v1/auth/logout", {
      method: "POST",
      token,
      body: refreshToken ? { refreshToken } : {},
    }),
};

