import type { Recommendation } from "@/types/api";

type RecommendationBadgeProps = {
  recommendation?: Recommendation | string | null;
  label?: string;
};

export function RecommendationBadge({ recommendation, label }: RecommendationBadgeProps) {
  if (!recommendation) {
    return null;
  }

  const map: Record<Recommendation, { label: string; style: string }> = {
    APPLY: { label: "Alta prioridade", style: "bg-emerald-100 text-emerald-800 border-emerald-200" },
    REVIEW: { label: "Revisar", style: "bg-amber-100 text-amber-800 border-amber-200" },
    IGNORE: { label: "Ignorar", style: "bg-slate-100 text-slate-700 border-slate-200" },
  };

  const token = map[recommendation as Recommendation];
  if (!token) {
    return (
      <span className="inline-flex rounded-full border border-slate-200 bg-slate-50 px-2.5 py-1 text-xs font-bold text-slate-600">
        Avaliacao indisponivel
      </span>
    );
  }

  return <span className={`inline-flex rounded-full border px-2.5 py-1 text-xs font-bold ${token.style}`}>{label ?? token.label}</span>;
}

