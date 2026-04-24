"use client";

import { useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { vacanciesApi } from "@/lib/api/vacancies";
import { loadMatchesProgressive, type MatchLoadState } from "@/lib/api/match-adapter";
import { useAuth } from "@/hooks/useAuth";
import { SummaryCard } from "@/components/dashboard/SummaryCard";
import { LoadingState } from "@/components/ui/LoadingState";
import { ErrorState } from "@/components/ui/ErrorState";
import { Card } from "@/components/ui/Card";
import { RecommendationBadge } from "@/components/matching/RecommendationBadge";
import type { MatchAnalysisResponse, VacancyListItem } from "@/types/api";

export default function DashboardHomePage() {
  const { requireToken, session } = useAuth();
  const [vacancies, setVacancies] = useState<VacancyListItem[]>([]);
  const [matches, setMatches] = useState<Record<string, MatchAnalysisResponse>>({});
  const [matchStates, setMatchStates] = useState<Record<string, MatchLoadState>>({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      setLoading(true);
      setError(null);
      setMatches({});
      setMatchStates({});

      try {
        const token = requireToken();
        const page = await vacanciesApi.list(token, { size: 12, sortBy: "createdAt", sortDirection: "desc" });

        if (cancelled) {
          return;
        }

        setVacancies(page.items);
        setLoading(false);

        const scopeKey = session?.user.id ?? "anonymous";
        await loadMatchesProgressive({
          token,
          vacancyIds: page.items.map((vacancy) => vacancy.id),
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
          setError("Falha ao carregar dashboard. Confirme backend disponível e sessão válida.");
          setLoading(false);
        }
      }
    }

    void load();

    return () => {
      cancelled = true;
    };
  }, [requireToken, session?.user.id]);

  const metrics = useMemo(() => {
    const values = Object.values(matches);
    return {
      total: vacancies.length,
      apply: values.filter((m) => m.recommendation === "APPLY").length,
      review: values.filter((m) => m.recommendation === "REVIEW").length,
      ignore: values.filter((m) => m.recommendation === "IGNORE").length,
    };
  }, [matches, vacancies.length]);

  if (loading) {
    return <LoadingState label="Carregando visão inicial..." />;
  }

  if (error) {
    return <ErrorState message={error} />;
  }

  return (
    <div className="space-y-5">
      <header>
        <h2 className="text-2xl font-bold">Dashboard operacional</h2>
        <p className="text-sm text-slate-600">Visão rápida para decidir onde aplicar primeiro.</p>
      </header>

      <section className="grid grid-cols-1 gap-3 md:grid-cols-2 xl:grid-cols-4">
        <SummaryCard title="Vagas analisadas" value={metrics.total} />
        <SummaryCard title="Recomendadas" value={metrics.apply} hint="APPLY" />
        <SummaryCard title="Para revisar" value={metrics.review} hint="REVIEW" />
        <SummaryCard title="Baixa aderência" value={metrics.ignore} hint="IGNORE" />
      </section>

      <section className="space-y-3">
        <div className="flex items-center justify-between">
          <h3 className="text-lg font-semibold">Últimas vagas</h3>
          <Link href="/ranking" className="text-sm font-semibold text-teal-700 hover:text-teal-600">
            Ver ranking completo
          </Link>
        </div>

        <div className="grid grid-cols-1 gap-3 lg:grid-cols-2">
          {vacancies.slice(0, 6).map((vacancy) => {
            const match = matches[vacancy.id];
            const matchState = matchStates[vacancy.id];
            return (
              <Card key={vacancy.id} className="p-4">
                <div className="flex items-start justify-between gap-2">
                  <div>
                    <p className="font-semibold text-slate-900">{vacancy.title}</p>
                    <p className="text-sm text-slate-600">{vacancy.company}</p>
                  </div>
                  {match ? <RecommendationBadge recommendation={match.recommendation} /> : null}
                </div>
                <p className="mt-2 text-sm text-slate-600">
                  Score:{" "}
                  {match
                    ? match.score
                    : matchState?.status === "loading"
                    ? "Carregando..."
                    : matchState?.status === "resume_missing"
                    ? "Currículo ausente"
                    : matchState?.status === "variant_missing"
                    ? "Variante ausente"
                    : matchState?.status === "not_generated"
                    ? "Nao gerado"
                    : matchState?.status === "not_found"
                    ? "Indisponível"
                    : "-"}
                </p>
                <Link href={`/vagas/${vacancy.id}`} className="mt-3 inline-block text-sm font-semibold text-teal-700 hover:text-teal-600">
                  Abrir vaga
                </Link>
              </Card>
            );
          })}
        </div>
      </section>
    </div>
  );
}
