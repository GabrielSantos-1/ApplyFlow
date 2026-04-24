import type { ApplicationStatus, TrackingStage } from "@/types/api";

export const STATUS_LABELS: Record<ApplicationStatus, string> = {
  DRAFT: "Preparando candidatura",
  READY_FOR_REVIEW: "Pronto para revisar",
  APPLIED: "Candidatura enviada",
  INTERVIEW: "Em analise",
  REJECTED: "Encerrado",
  OFFER: "Encerrado",
  WITHDRAWN: "Encerrado",
};

export const TRACKING_LABELS: Record<TrackingStage, string> = {
  CREATED: "Preparando candidatura",
  SUBMITTED: "Candidatura enviada",
  SCREENING: "Em analise",
  INTERVIEW: "Em analise",
  FINAL: "Em analise",
  CLOSED: "Encerrado",
};

const PRIMARY_NEXT_STATUS: Record<ApplicationStatus, ApplicationStatus | null> = {
  DRAFT: "READY_FOR_REVIEW",
  READY_FOR_REVIEW: "APPLIED",
  APPLIED: "INTERVIEW",
  INTERVIEW: null,
  OFFER: null,
  REJECTED: null,
  WITHDRAWN: null,
};

export const TIMELINE_STEPS = [
  "Preparando candidatura",
  "Pronto para revisar",
  "Candidatura enviada",
  "Em analise",
  "Encerrado",
] as const;

export function labelForStatus(status: ApplicationStatus): string {
  return STATUS_LABELS[status];
}

export function labelForTrackingStage(stage: TrackingStage): string {
  return TRACKING_LABELS[stage];
}

export function getPrimaryNextStatus(status: ApplicationStatus): ApplicationStatus | null {
  return PRIMARY_NEXT_STATUS[status];
}

export function getRecommendedNextStep(status: ApplicationStatus): string {
  if (status === "DRAFT") {
    return "Revise a candidatura antes do envio.";
  }
  if (status === "READY_FOR_REVIEW") {
    return "Confirmar envio manual da candidatura.";
  }
  if (status === "APPLIED") {
    return "Acompanhar retorno e registrar avancos.";
  }
  if (status === "INTERVIEW") {
    return "Registrar resultado da etapa e fechar ciclo quando concluir.";
  }
  return "Fluxo encerrado para esta candidatura.";
}

export function timelineIndexFromStatus(status: ApplicationStatus): number {
  if (status === "DRAFT") {
    return 0;
  }
  if (status === "READY_FOR_REVIEW") {
    return 1;
  }
  if (status === "APPLIED") {
    return 2;
  }
  if (status === "INTERVIEW") {
    return 3;
  }
  return 4;
}

