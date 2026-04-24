import type { ApiError } from "@/lib/api/client";
import type { MatchAnalysisResponse, Recommendation, VacancyListItem } from "@/types/api";

export type PriorityTone = "high" | "medium" | "low" | "unknown";

export type SafePriority =
  | {
      safe: true;
      recommendation: Recommendation;
      label: string;
      tone: PriorityTone;
    }
  | {
      safe: false;
      label: string;
      tone: "unknown";
      reason: string;
    };

const VALID_RECOMMENDATIONS = new Set(["APPLY", "REVIEW", "IGNORE"]);

export function getSafePriority(vacancy: VacancyListItem, match?: MatchAnalysisResponse): SafePriority {
  if (!match || match.state !== "GENERATED") {
    return unsafePriority("Nao foi possivel avaliar esta vaga com seguranca.");
  }
  if (match.vacancyId !== vacancy.id || vacancy.status !== "PUBLISHED") {
    return unsafePriority("Nao foi possivel avaliar esta vaga com seguranca.");
  }
  if (typeof match.score !== "number" || !Number.isFinite(match.score)) {
    return unsafePriority("Nao foi possivel avaliar esta vaga com seguranca.");
  }
  if (!match.recommendation || !VALID_RECOMMENDATIONS.has(match.recommendation)) {
    return unsafePriority("Nao foi possivel avaliar esta vaga com seguranca.");
  }
  if (match.recommendation === "APPLY") {
    return { safe: true, recommendation: "APPLY", label: "Alta prioridade", tone: "high" };
  }
  if (match.recommendation === "REVIEW") {
    return { safe: true, recommendation: "REVIEW", label: "Revisar", tone: "medium" };
  }
  return { safe: true, recommendation: "IGNORE", label: "Ignorar", tone: "low" };
}

export function compareVacanciesByBackendSignals(
  matches: Record<string, MatchAnalysisResponse>,
  a: VacancyListItem,
  b: VacancyListItem
): number {
  const scoreDiff = scoreForSort(matches[b.id]) - scoreForSort(matches[a.id]);
  if (scoreDiff !== 0) {
    return scoreDiff;
  }

  const publishedDiff = dateForSort(b.publishedAt) - dateForSort(a.publishedAt);
  if (publishedDiff !== 0) {
    return publishedDiff;
  }

  return qualityForSort(b) - qualityForSort(a);
}

export function mapVacancyActionError(error: unknown): string {
  const status = typeof (error as ApiError | undefined)?.status === "number" ? (error as ApiError).status : null;
  if (status === 400) {
    return "Acao invalida ou estado inconsistente.";
  }
  if (status === 401) {
    return "Sessao expirada. Entre novamente.";
  }
  if (status === 403) {
    return "Acesso negado para esta acao.";
  }
  if (status === 404) {
    return "Recurso inexistente ou sem permissao de acesso.";
  }
  if (status === 500) {
    return "Erro inesperado. Tente novamente mais tarde.";
  }
  return "Nao foi possivel concluir a acao.";
}

function unsafePriority(reason: string): SafePriority {
  return {
    safe: false,
    label: "Avaliacao indisponivel",
    tone: "unknown",
    reason,
  };
}

function scoreForSort(match?: MatchAnalysisResponse): number {
  return typeof match?.score === "number" && Number.isFinite(match.score) ? match.score : -1;
}

function dateForSort(value: string | null): number {
  if (!value) {
    return 0;
  }
  const time = new Date(value).getTime();
  return Number.isNaN(time) ? 0 : time;
}

function qualityForSort(vacancy: VacancyListItem): number {
  const maybeQuality = (vacancy as VacancyListItem & { qualityScore?: unknown }).qualityScore;
  return typeof maybeQuality === "number" && Number.isFinite(maybeQuality) ? maybeQuality : -1;
}
