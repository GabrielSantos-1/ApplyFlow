"use client";

import { useEffect, useMemo, useState } from "react";
import { applicationsApi } from "@/lib/api/applications";
import { vacanciesApi } from "@/lib/api/vacancies";
import { loadMatchesProgressive, type MatchLoadState } from "@/lib/api/match-adapter";
import { resumesApi } from "@/lib/api/resumes";
import { useAuth } from "@/hooks/useAuth";
import { LoadingState } from "@/components/ui/LoadingState";
import { ErrorState } from "@/components/ui/ErrorState";
import { Input } from "@/components/ui/Input";
import { VacancyListCard } from "@/components/vacancies/VacancyListCard";
import { compareVacanciesByBackendSignals, mapVacancyActionError } from "@/lib/vacancies/prioritization";
import type { ApplicationDraftResponse, MatchAnalysisResponse, ResumeResponse, VacancyListItem } from "@/types/api";

export default function VagasPage() {
  const { requireToken, session } = useAuth();
  const [queryInput, setQueryInput] = useState("");
  const [query, setQuery] = useState("");
  const [workModel, setWorkModel] = useState<"all" | "remote" | "hybrid" | "onsite">("all");
  const [seniority, setSeniority] = useState<"all" | "junior" | "pleno" | "senior" | "especialista">("all");
  const [vacancies, setVacancies] = useState<VacancyListItem[]>([]);
  const [matches, setMatches] = useState<Record<string, MatchAnalysisResponse>>({});
  const [matchStates, setMatchStates] = useState<Record<string, MatchLoadState>>({});
  const [applicationsByVacancy, setApplicationsByVacancy] = useState<Record<string, ApplicationDraftResponse>>({});
  const [resumes, setResumes] = useState<ResumeResponse[]>([]);
  const [applyStates, setApplyStates] = useState<Record<string, "idle" | "loading" | "created">>({});
  const [ignoredVacancyIds, setIgnoredVacancyIds] = useState<Set<string>>(() => new Set());
  const [vacanciesLoading, setVacanciesLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  useEffect(() => {
    const timer = window.setTimeout(() => setQuery(queryInput.trim()), 350);
    return () => window.clearTimeout(timer);
  }, [queryInput]);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      setVacanciesLoading(true);
      setError(null);
      setMatches({});
      setMatchStates({});

      try {
        const token = requireToken();
        const [page, applicationsPage, resumesPage] = await Promise.all([
          vacanciesApi.list(token, {
            query,
            workModel,
            seniority,
            size: 20,
            sortBy: "createdAt",
            sortDirection: "desc",
          }),
          applicationsApi.list(token, 0, 100),
          resumesApi.list(token, 0, 20),
        ]);

        if (cancelled) {
          return;
        }

        setVacancies(page.items);
        setResumes(resumesPage.items);
        setApplicationsByVacancy(indexApplicationsByVacancy(applicationsPage.items));
        setApplyStates((prev) => mergeCreatedApplyStates(prev, applicationsPage.items));
        setVacanciesLoading(false);

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
      } catch (loadError) {
        if (!cancelled) {
          setError(mapVacancyActionError(loadError));
          setVacanciesLoading(false);
        }
      }
    }

    void load();

    return () => {
      cancelled = true;
    };
  }, [query, workModel, seniority, requireToken, session?.user.id]);

  const sortedVacancies = useMemo(() => {
    return [...vacancies]
      .filter((vacancy) => !ignoredVacancyIds.has(vacancy.id))
      .sort((a, b) => compareVacanciesByBackendSignals(matches, a, b));
  }, [vacancies, matches, ignoredVacancyIds]);

  async function applyToVacancy(vacancy: VacancyListItem) {
    if (applyStates[vacancy.id] === "loading" || applyStates[vacancy.id] === "created") {
      return;
    }
    if (applicationsByVacancy[vacancy.id]) {
      setApplyStates((prev) => ({ ...prev, [vacancy.id]: "created" }));
      return;
    }
    if (resumes.length === 0) {
      setActionError("Envie um curriculo base antes de criar um draft de candidatura.");
      return;
    }

    setActionError(null);
    setApplyStates((prev) => ({ ...prev, [vacancy.id]: "loading" }));
    try {
      const draft = await applicationsApi.createDraftAssisted(requireToken(), {
        vacancyId: vacancy.id,
      });
      setApplicationsByVacancy((prev) => ({ ...prev, [draft.vacancyId]: draft }));
      setApplyStates((prev) => ({ ...prev, [vacancy.id]: "created" }));
    } catch (actionFailure) {
      setActionError(mapVacancyActionError(actionFailure));
      setApplyStates((prev) => ({ ...prev, [vacancy.id]: "idle" }));
    }
  }

  function ignoreVacancy(vacancyId: string) {
    setIgnoredVacancyIds((prev) => {
      const next = new Set(prev);
      next.add(vacancyId);
      return next;
    });
    setActionError(null);
  }

  function clearFilters() {
    setQueryInput("");
    setQuery("");
    setWorkModel("all");
    setSeniority("all");
    setActionError(null);
  }

  const hasActiveFilters = queryInput.trim() !== "" || workModel !== "all" || seniority !== "all";
  const visibleVacanciesCount = sortedVacancies.length;
  const totalVacanciesCount = vacancies.length;

  return (
    <div className="mx-auto flex w-full max-w-6xl flex-col gap-5 px-1 pb-10 md:px-2">
      <header className="overflow-hidden rounded-3xl border border-slate-200 bg-white shadow-sm">
        <div className="border-b border-slate-100 bg-[radial-gradient(circle_at_top_left,_rgba(20,184,166,0.16),_transparent_36%),linear-gradient(135deg,_#ffffff,_#f8fafc)] px-5 py-6 md:px-8 md:py-8">
          <div className="mx-auto max-w-3xl text-center">
            <p className="text-xs font-bold uppercase tracking-[0.22em] text-teal-700">Dashboard de oportunidades</p>
            <h2 className="mt-3 text-3xl font-bold tracking-tight text-slate-950 md:text-4xl">Vagas</h2>
            <p className="mt-3 text-sm leading-6 text-slate-600 md:text-base">
              Priorize oportunidades com base no score, recomendacao e dados retornados pelo backend.
            </p>
          </div>

          <div className="mt-6 grid grid-cols-1 gap-3 text-center sm:grid-cols-3">
            <MetricPill label="Carregadas" value={String(totalVacanciesCount)} />
            <MetricPill label="Visíveis" value={String(visibleVacanciesCount)} />
            <MetricPill label="Ignoradas" value={String(ignoredVacancyIds.size)} />
          </div>
        </div>
      </header>

      <section className="rounded-3xl border border-slate-200 bg-white p-4 shadow-sm md:p-5" aria-labelledby="vagas-filtros-title">
        <div className="mb-4 flex flex-col gap-1 md:flex-row md:items-end md:justify-between">
          <div>
            <h3 id="vagas-filtros-title" className="text-sm font-bold text-slate-900">
              Busca e filtros
            </h3>
            <p className="text-xs text-slate-500">Refine a lista sem alterar o ranking calculado no backend.</p>
          </div>
          {hasActiveFilters ? (
            <button
              type="button"
              onClick={clearFilters}
              className="inline-flex h-10 items-center justify-center rounded-xl border border-slate-200 px-4 text-sm font-semibold text-slate-600 transition hover:border-slate-300 hover:bg-slate-50 focus:outline-none focus:ring-4 focus:ring-teal-500/20"
            >
              Limpar filtros
            </button>
          ) : null}
        </div>

        <div className="grid grid-cols-1 gap-3 lg:grid-cols-[minmax(0,1fr)_190px_190px]">
          <label className="block">
            <span className="sr-only">Buscar por cargo, empresa ou tecnologia</span>
            <Input
              aria-label="Buscar por cargo, empresa ou tecnologia"
              placeholder="Buscar por cargo, empresa ou tecnologia..."
              value={queryInput}
              onChange={(e) => setQueryInput(e.target.value)}
              className="h-12 rounded-2xl border-slate-200 bg-slate-50 px-4 text-base focus:bg-white"
            />
          </label>

          <label className="block">
            <span className="sr-only">Modelo de trabalho</span>
            <select
              aria-label="Filtrar por modelo de trabalho"
              className="h-12 w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 text-sm font-medium text-slate-700 outline-none transition focus:border-teal-500 focus:bg-white focus:ring-4 focus:ring-teal-500/20"
              value={workModel}
              onChange={(e) => setWorkModel(e.target.value as typeof workModel)}
            >
              <option value="all">Modelo de trabalho</option>
              <option value="remote">Remoto</option>
              <option value="hybrid">Hibrido</option>
              <option value="onsite">Presencial</option>
            </select>
          </label>

          <label className="block">
            <span className="sr-only">Senioridade</span>
            <select
              aria-label="Filtrar por senioridade"
              className="h-12 w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 text-sm font-medium text-slate-700 outline-none transition focus:border-teal-500 focus:bg-white focus:ring-4 focus:ring-teal-500/20"
              value={seniority}
              onChange={(e) => setSeniority(e.target.value as typeof seniority)}
            >
              <option value="all">Senioridade</option>
              <option value="junior">Junior</option>
              <option value="pleno">Pleno</option>
              <option value="senior">Senior</option>
              <option value="especialista">Especialista</option>
            </select>
          </label>
        </div>
      </section>

      {error ? <ErrorState message="Não foi possível carregar as vagas agora. Tente novamente ou verifique a disponibilidade do backend." /> : null}
      {actionError ? <ErrorState message={actionError} /> : null}

      <section className="rounded-3xl border border-slate-200 bg-white p-4 shadow-sm md:p-5" aria-labelledby="vagas-lista-title">
        <div className="mb-4 flex flex-col gap-1 border-b border-slate-100 pb-4 md:flex-row md:items-end md:justify-between">
          <div>
            <h3 id="vagas-lista-title" className="text-base font-bold text-slate-950">
              Oportunidades encontradas
            </h3>
            <p className="text-sm text-slate-500">
              {vacanciesLoading
                ? "Carregando vagas..."
                : `${visibleVacanciesCount} oportunidade${visibleVacanciesCount === 1 ? "" : "s"} visível${visibleVacanciesCount === 1 ? "" : "eis"}.`}
            </p>
          </div>
        </div>

        {vacanciesLoading ? <LoadingState label="Carregando vagas..." /> : null}

        {!vacanciesLoading && !error && sortedVacancies.length === 0 ? (
          <div className="rounded-2xl border border-dashed border-slate-300 bg-slate-50 p-8 text-center">
            <p className="text-sm font-semibold text-slate-800">Nenhuma vaga encontrada.</p>
            <p className="mt-2 text-sm text-slate-600">Ajuste os filtros ou execute uma nova ingestão de vagas.</p>
          </div>
        ) : null}

        <div className="grid grid-cols-1 gap-4">
          {sortedVacancies.map((vacancy) => (
            <VacancyListCard
              key={vacancy.id}
              vacancy={vacancy}
              match={matches[vacancy.id]}
              matchState={matchStates[vacancy.id]}
              applyState={applyStates[vacancy.id] ?? (applicationsByVacancy[vacancy.id] ? "created" : "idle")}
              ignored={ignoredVacancyIds.has(vacancy.id)}
              onApply={(item) => void applyToVacancy(item)}
              onIgnore={ignoreVacancy}
            />
          ))}
        </div>
      </section>
    </div>
  );
}

function MetricPill({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-2xl border border-white/70 bg-white/75 px-4 py-3 shadow-sm backdrop-blur">
      <p className="text-2xl font-bold text-slate-950">{value}</p>
      <p className="text-xs font-semibold uppercase tracking-[0.16em] text-slate-500">{label}</p>
    </div>
  );
}

function indexApplicationsByVacancy(items: ApplicationDraftResponse[]): Record<string, ApplicationDraftResponse> {
  const output: Record<string, ApplicationDraftResponse> = {};
  items.forEach((item) => {
    output[item.vacancyId] = item;
  });
  return output;
}

function mergeCreatedApplyStates(
  current: Record<string, "idle" | "loading" | "created">,
  applications: ApplicationDraftResponse[]
): Record<string, "idle" | "loading" | "created"> {
  const next = { ...current };
  applications.forEach((application) => {
    next[application.vacancyId] = "created";
  });
  return next;
}
