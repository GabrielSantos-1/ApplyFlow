import Link from "next/link";
import type { MatchLoadState } from "@/lib/api/match-adapter";
import { RecommendationBadge } from "@/components/matching/RecommendationBadge";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { formatDate } from "@/lib/utils";
import { getSafePriority } from "@/lib/vacancies/prioritization";
import type { MatchAnalysisResponse, VacancyListItem } from "@/types/api";

type VacancyListCardProps = {
  vacancy: VacancyListItem;
  match?: MatchAnalysisResponse;
  matchState?: MatchLoadState;
  applyState?: "idle" | "loading" | "created";
  ignored?: boolean;
  onApply?: (vacancy: VacancyListItem) => void;
  onIgnore?: (vacancyId: string) => void;
};

export function VacancyListCard({
  vacancy,
  match,
  matchState,
  applyState = "idle",
  ignored = false,
  onApply,
  onIgnore,
}: VacancyListCardProps) {
  const status = matchState?.status ?? (match ? "success" : "idle");
  const priority = getSafePriority(vacancy, match);
  const normalizedSeniority = vacancy.seniority?.trim() || "Nao informado";
  const matchHint =
    status === "resume_missing"
      ? "Envie um curriculo base para habilitar analise."
      : status === "variant_missing"
      ? "Crie uma variante para esta vaga no detalhe."
      : status === "not_generated"
      ? "Gere o match explicitamente no detalhe da vaga."
      : status === "not_found"
      ? "Analise ainda nao disponivel."
      : status === "rate_limited"
      ? "Limite temporario atingido."
      : status === "error"
      ? "Falha momentanea na analise."
      : null;
  const scoreLabel =
    status === "loading"
      ? "Carregando..."
      : status === "resume_missing"
      ? "Curriculo ausente"
      : status === "variant_missing"
      ? "Variante ausente"
      : status === "not_generated"
      ? "Nao gerado"
      : status === "not_found"
      ? "Indisponivel"
      : status === "rate_limited"
      ? "429 temporario"
      : status === "error"
      ? "Falha"
      : match
      ? String(match.score)
      : "-";
  const cardTone =
    priority.safe && priority.tone === "high"
      ? "border-l-4 border-l-emerald-500"
      : priority.safe && priority.tone === "medium"
      ? "border-l-4 border-l-amber-500"
      : priority.safe && priority.tone === "low"
      ? "border-l-4 border-l-slate-300 opacity-80"
      : "border-l-4 border-l-slate-200";
  const canApply = priority.safe && applyState !== "loading" && applyState !== "created";

  return (
    <Card className={`p-4 ${cardTone}`}>
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h3 className="text-base font-semibold text-slate-900">{vacancy.title}</h3>
          <p className="text-sm text-slate-600">
            {vacancy.company} - {vacancy.location || "Local nao informado"}
          </p>
          <p className="mt-1 text-xs text-slate-500">Senioridade: {normalizedSeniority}</p>
          <p className="mt-1 text-xs text-slate-500">
            Publicada em: {vacancy.publishedAt ? formatDate(vacancy.publishedAt) : "N/D"}
          </p>
        </div>

        <div className="text-right">
          {priority.safe ? (
            <RecommendationBadge recommendation={priority.recommendation} label={priority.label} />
          ) : (
            <RecommendationBadge recommendation="UNKNOWN" />
          )}
          <p className="mt-2 text-sm font-semibold text-slate-900">Score: {scoreLabel}</p>
          {matchHint ? <p className="mt-1 text-xs text-slate-500">{matchHint}</p> : null}
        </div>
      </div>

      {!priority.safe ? (
        <p className="mt-3 rounded-lg border border-slate-200 bg-slate-50 p-3 text-sm text-slate-700">{priority.reason}</p>
      ) : null}

      {match ? (
        <div className="mt-3 grid grid-cols-1 gap-3 md:grid-cols-2">
          <SignalList title="Strengths" items={match.strengths.slice(0, 3)} emptyLabel="Sem pontos fortes retornados." />
          <SignalList title="Gaps" items={match.gaps.slice(0, 3)} emptyLabel="Sem gaps retornados." />
        </div>
      ) : null}

      <div className="mt-4 flex flex-wrap items-center gap-2">
        <Button type="button" variant="secondary" onClick={() => onApply?.(vacancy)} disabled={!canApply}>
          {applyState === "loading" ? "Criando..." : applyState === "created" ? "Draft criado" : "Aplicar"}
        </Button>
        <Link href={`/vagas/${vacancy.id}`} className="inline-flex items-center rounded-lg px-3 py-2 text-sm font-semibold text-teal-700 hover:bg-teal-50">
          Revisar
        </Link>
        <Button type="button" variant="ghost" onClick={() => onIgnore?.(vacancy.id)} disabled={applyState === "loading" || ignored}>
          {ignored ? "Ignorada" : "Ignorar"}
        </Button>
        {vacancy.jobUrl ? (
          <a href={vacancy.jobUrl} target="_blank" rel="noopener noreferrer" className="text-sm font-semibold text-slate-700 hover:text-slate-900">
            Vaga original
          </a>
        ) : null}
      </div>
    </Card>
  );
}

function SignalList({ title, items, emptyLabel }: { title: string; items: string[]; emptyLabel: string }) {
  return (
    <div className="rounded-lg border border-slate-200 bg-slate-50 p-3">
      <p className="text-xs font-bold uppercase tracking-wide text-slate-500">{title}</p>
      <ul className="mt-2 space-y-1 text-sm text-slate-700">
        {items.length ? items.map((item) => <li key={item}>{item}</li>) : <li>{emptyLabel}</li>}
      </ul>
    </div>
  );
}
