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

  return (
    <div className="space-y-4">
      <header>
        <h2 className="text-2xl font-bold">Vagas</h2>
        <p className="text-sm text-slate-600">Priorizacao baseada somente em score, recomendacao e datas retornados pelo backend.</p>
      </header>

      <section className="grid grid-cols-1 gap-2 md:grid-cols-4">
        <Input aria-label="Buscar vagas por termo" placeholder="Buscar por termo" value={queryInput} onChange={(e) => setQueryInput(e.target.value)} />

        <select
          aria-label="Filtrar por modelo de trabalho"
          className="rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm"
          value={workModel}
          onChange={(e) => setWorkModel(e.target.value as typeof workModel)}
        >
          <option value="all">Modelo de trabalho</option>
          <option value="remote">Remoto</option>
          <option value="hybrid">Hibrido</option>
          <option value="onsite">Presencial</option>
        </select>

        <select
          aria-label="Filtrar por senioridade"
          className="rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm"
          value={seniority}
          onChange={(e) => setSeniority(e.target.value as typeof seniority)}
        >
          <option value="all">Senioridade</option>
          <option value="junior">Junior</option>
          <option value="pleno">Pleno</option>
          <option value="senior">Senior</option>
          <option value="especialista">Especialista</option>
        </select>
      </section>

      {vacanciesLoading ? <LoadingState label="Carregando vagas..." /> : null}
      {error ? <ErrorState message={error} /> : null}
      {actionError ? <ErrorState message={actionError} /> : null}

      {!vacanciesLoading && !error && sortedVacancies.length === 0 ? (
        <div className="rounded-lg border border-slate-200 bg-white p-6 text-sm text-slate-600">Nenhuma vaga encontrada com os filtros.</div>
      ) : null}

      <section className="grid grid-cols-1 gap-3">
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
      </section>
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
