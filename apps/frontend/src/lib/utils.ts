import type { Recommendation } from "@/types/api";

const DATE_FORMATTER = new Intl.DateTimeFormat("pt-BR", {
  dateStyle: "medium",
});

export function formatDate(value: string | Date): string {
  const date = value instanceof Date ? value : new Date(value);
  return Number.isNaN(date.getTime()) ? "-" : DATE_FORMATTER.format(date);
}

export function recommendationLabel(value: Recommendation): string {
  if (value === "APPLY") return "Aplicar";
  if (value === "REVIEW") return "Revisar";
  return "Ignorar";
}

