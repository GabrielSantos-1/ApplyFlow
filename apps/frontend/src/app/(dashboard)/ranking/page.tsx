"use client";

import { useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { vacanciesApi } from "@/lib/api/vacancies";
import { loadMatchesProgressive, type MatchLoadState } from "@/lib/api/match-adapter";
import { useAuth } from "@/hooks/useAuth";
import { LoadingState } from "@/components/ui/LoadingState";
import { ErrorState } from "@/components/ui/ErrorState";
import { Card } from "@/components/ui/Card";
import { RecommendationBadge } from "@/components/matching/RecommendationBadge";
import type { MatchAnalysisResponse, VacancyListItem } from "@/types/api";

export default function RankingPage() {
  const { requireToken, session } = useAuth();
  const [vacancies, setVacancies] = useState<VacancyListItem[]>([]);
  const [matches, setMatches] = useState<Record<string, MatchAnalysisResponse>>({});
  const [matchStates, setMatchStates] = useState<Record<string, MatchLoadState>>({});
  const [loadingVacancies, setLoadingVacancies] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      setLoadingVacancies(true);
      setError(null);
      setVacancies([]);
      setMatches({});
      setMatchStates({});

      try {
        const token = requireToken();
        const vacanciesPage = await vacanciesApi.list(token, { size: 25, sortBy: "createdAt", sortDirection: "desc" });

        if (cancelled) {
          return;
        }

        setVacancies(vacanciesPage.items);
        setLoadingVacancies(false);

        const scopeKey = session?.user.id ?? "anonymous";
        await loadMatchesProgressive({
          token,
          vacancyIds: vacanciesPage.items.map((vacancy) => vacancy.id),
          scopeKey,
          concurrency: 2,
          retry429: 1,
          onItem: (vacancyId, state) => {
            if (cancelled) {
              return;
            }
            setMatchStates((prev) => ({ ...prev, [vacancyId]: state }));
            if (state.status === "success") {
              setMatches((prev) => ({ ...prev, [vacancyId]: state.data }));
            }
          },
        });
      } catch {
        if (!cancelled) {
          setError("Falha ao carregar ranking de vagas.");
          setLoadingVacancies(false);
        }
      }
    }

    void load();
    return () => {
      cancelled = true;
    };
  }, [requireToken, session?.user.id]);

  const sorted = useMemo(
    () =>
      [...vacancies].sort((a, b) => {
        const scoreA = matches[a.id]?.score ?? -1;
        const scoreB = matches[b.id]?.score ?? -1;
        return scoreB - scoreA;
      }),
    [vacancies, matches]
  );

  if (loadingVacancies) return <LoadingState label="Calculando ranking..." />;
  if (error) return <ErrorState message={error} />;

  return (
    <div className="space-y-4">
      <header>
        <h2 className="text-2xl font-bold">Ranking de Prioridade</h2>
        <p className="text-sm text-slate-600">Ordenação por score determinístico para decisão de aplicação.</p>
      </header>

      <div className="space-y-2">
        {sorted.map((vacancy, index) => {
          const match = matches[vacancy.id];
          const matchState = matchStates[vacancy.id];

          return (
            <Card key={vacancy.id} className="flex items-center justify-between gap-3 p-4">
              <div>
                <p className="text-xs font-semibold uppercase text-slate-500">#{index + 1}</p>
                <p className="font-semibold text-slate-900">{vacancy.title}</p>
                <p className="text-sm text-slate-600">{vacancy.company}</p>
              </div>

              <div className="text-right">
                {match ? <RecommendationBadge recommendation={match.recommendation} /> : null}
                <p className="mt-1 text-lg font-bold text-slate-900">
                  {match
                    ? match.score
                    : matchState?.status === "loading"
                    ? "..."
                    : matchState?.status === "resume_missing"
                    ? "CV"
                    : matchState?.status === "variant_missing"
                    ? "VAR"
                    : matchState?.status === "not_generated"
                    ? "N/G"
                    : matchState?.status === "not_found"
                    ? "N/D"
                    : matchState?.status === "rate_limited"
                    ? "429"
                    : "-"}
                </p>
                <Link href={`/vagas/${vacancy.id}`} className="text-sm font-semibold text-teal-700 hover:text-teal-600">
                  Abrir detalhe
                </Link>
              </div>
            </Card>
          );
        })}
      </div>
    </div>
  );
}
