import type { ApiErrorBody } from "@/types/api";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8081";

export class ApiError extends Error {
  status: number;
  body?: ApiErrorBody | string;

  constructor(status: number, message: string, body?: ApiErrorBody | string) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.body = body;
  }
}

type HttpMethod = "GET" | "POST" | "PATCH";

type RequestOptions = {
  method?: HttpMethod;
  body?: unknown;
  token?: string;
  signal?: AbortSignal;
};

export async function apiRequest<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { method = "GET", body, token, signal } = options;
  const isFormData = body instanceof FormData;

  const headers = new Headers();
  headers.set("X-Requested-With", "applyflow-web");
  if (body !== undefined && !isFormData) {
    headers.set("Content-Type", "application/json");
  }
  if (token) {
    headers.set("Authorization", `Bearer ${token}`);
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    method,
    headers,
    credentials: "include",
    body:
      body === undefined
        ? undefined
        : isFormData
        ? (body as FormData)
        : JSON.stringify(body),
    signal,
    cache: "no-store",
  });

  if (response.status === 204) {
    return undefined as T;
  }

  const contentType = response.headers.get("content-type") ?? "";
  const isJson = contentType.includes("application/json");
  const parsedBody = isJson ? ((await response.json()) as ApiErrorBody) : await response.text();

  if (!response.ok) {
    const message =
      (typeof parsedBody === "object" && parsedBody !== null && parsedBody.message) ||
      `Falha na API (${response.status})`;
    throw new ApiError(response.status, message, parsedBody);
  }

  return parsedBody as T;
}

export { API_BASE_URL };

