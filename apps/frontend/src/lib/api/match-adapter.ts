import { ApiError } from "@/lib/api/client";
import { matchingApi } from "@/lib/api/matching";
import type { MatchAnalysisResponse } from "@/types/api";

export type MatchLoadState =
  | { status: "idle" }
  | { status: "loading" }
  | { status: "success"; data: MatchAnalysisResponse }
  | { status: "resume_missing"; message: string }
  | { status: "variant_missing"; message: string }
  | { status: "not_generated"; message: string }
  | { status: "not_found"; message: string }
  | { status: "rate_limited"; message: string }
  | { status: "error"; message: string };

type MatchOptions = {
  scopeKey: string;
  forceRefresh?: boolean;
  retry429?: number;
};

type ProgressiveLoadOptions = {
  token: string;
  vacancyIds: string[];
  scopeKey: string;
  concurrency?: number;
  retry429?: number;
  onItem?: (vacancyId: string, state: MatchLoadState) => void;
};

const CACHE = new Map<string, MatchAnalysisResponse>();
const INFLIGHT = new Map<string, Promise<MatchAnalysisResponse>>();

function cacheKey(scopeKey: string, vacancyId: string): string {
  return `${scopeKey}:${vacancyId}`;
}

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

function isRateLimited(error: unknown): error is ApiError {
  return error instanceof ApiError && error.status === 429;
}

function isNotFound(error: unknown): error is ApiError {
  return error instanceof ApiError && error.status === 404;
}

function parseApiMessage(error: ApiError): string {
  if (typeof error.body === "object" && error.body !== null && typeof error.body.message === "string") {
    return error.body.message;
  }
  return error.message || "";
}

function classifyNotFoundState(error: ApiError): MatchLoadState {
  const message = parseApiMessage(error).toLowerCase();
  if (message.includes("nenhum curriculo")) {
    return {
      status: "resume_missing",
      message: "Envie um currículo base para habilitar análise de match.",
    };
  }
  if (message.includes("nenhuma variante")) {
    return {
      status: "variant_missing",
      message: "Esta vaga ainda não possui variante de currículo associada.",
    };
  }
  return {
    status: "not_found",
    message: "Match ainda não disponível para esta vaga.",
  };
}

export function mapMatchErrorToState(error: unknown): MatchLoadState {
  if (isRateLimited(error)) {
    return {
      status: "rate_limited",
      message: "Limite temporário de análise atingido. Tente novamente em instantes.",
    };
  }
  if (isNotFound(error)) {
    return classifyNotFoundState(error);
  }
  return {
    status: "error",
    message: "Falha ao carregar match para esta vaga.",
  };
}

export function mapMatchResponseToState(data: MatchAnalysisResponse): MatchLoadState {
  if (data.state === "GENERATED") {
    return { status: "success", data };
  }
  if (data.state === "MISSING_RESUME") {
    return {
      status: "resume_missing",
      message: "Envie um curriculo base para habilitar analise de match.",
    };
  }
  if (data.state === "MISSING_VARIANT") {
    return {
      status: "variant_missing",
      message: "Esta vaga ainda nao possui variante de curriculo associada.",
    };
  }
  if (data.state === "NOT_GENERATED") {
    return {
      status: "not_generated",
      message: "Match ainda nao foi gerado para esta vaga.",
    };
  }
  return {
    status: "error",
    message: "Falha ao carregar match para esta vaga.",
  };
}

async function fetchWithRetry(token: string, vacancyId: string, retry429: number): Promise<MatchAnalysisResponse> {
  let attempt = 0;
  while (true) {
    try {
      return await matchingApi.byVacancy(token, vacancyId);
    } catch (error) {
      if (!isRateLimited(error) || attempt >= retry429) {
        throw error;
      }
      const backoffMs = 250 * 2 ** attempt + Math.floor(Math.random() * 150);
      await sleep(backoffMs);
      attempt += 1;
    }
  }
}

export async function getMatchForVacancy(
  token: string,
  vacancyId: string,
  options: MatchOptions
): Promise<MatchAnalysisResponse> {
  const key = cacheKey(options.scopeKey, vacancyId);
  if (!options.forceRefresh && CACHE.has(key)) {
    return CACHE.get(key)!;
  }

  const inflight = INFLIGHT.get(key);
  if (inflight) {
    return inflight;
  }

  const request = fetchWithRetry(token, vacancyId, options.retry429 ?? 1)
    .then((match) => {
      CACHE.set(key, match);
      return match;
    })
    .finally(() => {
      INFLIGHT.delete(key);
    });

  INFLIGHT.set(key, request);
  return request;
}

export async function loadMatchesProgressive(options: ProgressiveLoadOptions): Promise<void> {
  const { token, scopeKey, onItem } = options;
  const vacancyIds = [...new Set(options.vacancyIds)];
  const concurrency = Math.max(1, Math.min(options.concurrency ?? 3, 5));
  const retry429 = options.retry429 ?? 1;

  let cursor = 0;

  async function worker() {
    while (cursor < vacancyIds.length) {
      const index = cursor;
      cursor += 1;
      const vacancyId = vacancyIds[index];

      onItem?.(vacancyId, { status: "loading" });
      try {
        const data = await getMatchForVacancy(token, vacancyId, {
          scopeKey,
          retry429,
        });
        onItem?.(vacancyId, mapMatchResponseToState(data));
      } catch (error) {
        onItem?.(vacancyId, mapMatchErrorToState(error));
      }
    }
  }

  await Promise.all(Array.from({ length: concurrency }, () => worker()));
}

export function clearMatchScope(scopeKey: string): void {
  const prefix = `${scopeKey}:`;
  for (const key of [...CACHE.keys()]) {
    if (key.startsWith(prefix)) {
      CACHE.delete(key);
    }
  }
}
