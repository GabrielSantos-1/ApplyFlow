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
  const workModelLabel = vacancy.remote ? "Remoto" : "Modelo não informado";
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
  const scoreText = match ? `Score ${scoreLabel}` : scoreLabel;
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
    <Card className={`group overflow-hidden rounded-2xl border-slate-200 bg-white p-0 shadow-sm transition hover:-translate-y-0.5 hover:shadow-md ${cardTone}`}>
      <div className="p-4 md:p-5">
        <div className="flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
          <div className="min-w-0">
            <div className="flex flex-wrap items-center gap-2">
              <span className="rounded-full bg-slate-100 px-2.5 py-1 text-xs font-bold uppercase tracking-wide text-slate-600">
                {normalizedSeniority}
              </span>
              <span className="rounded-full bg-slate-100 px-2.5 py-1 text-xs font-semibold text-slate-600">
                {workModelLabel}
              </span>
            </div>

            <h3 className="mt-3 text-lg font-bold leading-snug text-slate-950 md:text-xl">{vacancy.title}</h3>
            <p className="mt-2 text-sm text-slate-600">
              <span className="font-semibold text-slate-800">{vacancy.company}</span>
              <span className="px-2 text-slate-300">•</span>
              <span>{vacancy.location || "Local não informado"}</span>
            </p>
            <p className="mt-1 text-xs text-slate-500">Publicada em: {vacancy.publishedAt ? formatDate(vacancy.publishedAt) : "N/D"}</p>
          </div>

          <div className="flex shrink-0 flex-row items-center justify-between gap-3 rounded-2xl border border-slate-100 bg-slate-50 px-3 py-3 md:min-w-44 md:flex-col md:items-end md:text-right">
            {priority.safe ? (
              <RecommendationBadge recommendation={priority.recommendation} label={priority.label} />
            ) : (
              <RecommendationBadge recommendation="UNKNOWN" />
            )}
            <div>
              <p className="text-xs font-semibold uppercase tracking-[0.16em] text-slate-500">Análise</p>
              <p className="mt-1 text-sm font-bold text-slate-950">{scoreText}</p>
            </div>
          </div>
        </div>

        {matchHint ? (
          <p className="mt-4 rounded-2xl border border-slate-200 bg-slate-50 p-3 text-sm text-slate-700">{matchHint}</p>
        ) : null}

        {!priority.safe ? (
          <p className="mt-4 rounded-2xl border border-slate-200 bg-slate-50 p-3 text-sm text-slate-700">{priority.reason}</p>
        ) : null}

        {match ? (
          <div className="mt-4 grid grid-cols-1 gap-3 md:grid-cols-2">
            <SignalList title="Pontos fortes" items={match.strengths.slice(0, 3)} emptyLabel="Sem pontos fortes retornados." />
            <SignalList title="Gaps" items={match.gaps.slice(0, 3)} emptyLabel="Sem gaps retornados." />
          </div>
        ) : null}
      </div>

      <div className="flex flex-col gap-2 border-t border-slate-100 bg-slate-50/70 px-4 py-4 sm:flex-row sm:flex-wrap sm:items-center md:px-5">
        <Button type="button" variant="secondary" onClick={() => onApply?.(vacancy)} disabled={!canApply} className="min-h-10 sm:min-w-32">
          {applyState === "loading" ? "Criando..." : applyState === "created" ? "Draft criado" : "Aplicar"}
        </Button>
        <Link
          href={`/vagas/${vacancy.id}`}
          className="inline-flex min-h-10 items-center justify-center rounded-lg border border-slate-200 bg-white px-4 py-2 text-sm font-semibold text-slate-800 transition hover:border-teal-200 hover:bg-teal-50 hover:text-teal-800 focus:outline-none focus:ring-4 focus:ring-teal-500/20"
        >
          Revisar
        </Link>
        <Button type="button" variant="ghost" onClick={() => onIgnore?.(vacancy.id)} disabled={applyState === "loading" || ignored} className="min-h-10">
          {ignored ? "Ignorada" : "Ignorar"}
        </Button>
        {vacancy.jobUrl ? (
          <a
            href={vacancy.jobUrl}
            target="_blank"
            rel="noopener noreferrer"
            className="inline-flex min-h-10 items-center justify-center rounded-lg px-4 py-2 text-sm font-semibold text-slate-700 transition hover:bg-white hover:text-slate-950 focus:outline-none focus:ring-4 focus:ring-teal-500/20"
          >
            Vaga original
          </a>
        ) : (
          <span className="inline-flex min-h-10 items-center justify-center rounded-lg px-4 py-2 text-sm font-medium text-slate-400">
            Vaga original indisponível
          </span>
        )}
      </div>
    </Card>
  );
}

function SignalList({ title, items, emptyLabel }: { title: string; items: string[]; emptyLabel: string }) {
  return (
    <div className="rounded-2xl border border-slate-200 bg-slate-50 p-3">
      <p className="text-xs font-bold uppercase tracking-[0.14em] text-slate-500">{title}</p>
      <ul className="mt-2 space-y-1.5 text-sm text-slate-700">
        {items.length ? (
          items.map((item) => (
            <li key={item} className="flex gap-2">
              <span className="mt-2 h-1.5 w-1.5 shrink-0 rounded-full bg-teal-500" aria-hidden="true" />
              <span>{item}</span>
            </li>
          ))
        ) : (
          <li>{emptyLabel}</li>
        )}
      </ul>
    </div>
  );
}
