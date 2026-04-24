"use client";

import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import { useParams } from "next/navigation";
import { vacanciesApi } from "@/lib/api/vacancies";
import { applicationsApi } from "@/lib/api/applications";
import { resumesApi } from "@/lib/api/resumes";
import { matchingApi } from "@/lib/api/matching";
import { getMatchForVacancy, mapMatchErrorToState, mapMatchResponseToState, type MatchLoadState } from "@/lib/api/match-adapter";
import { featureFlags } from "@/lib/config/features";
import { useAuth } from "@/hooks/useAuth";
import { LoadingState } from "@/components/ui/LoadingState";
import { ErrorState } from "@/components/ui/ErrorState";
import { Card } from "@/components/ui/Card";
import { RecommendationBadge } from "@/components/matching/RecommendationBadge";
import { Button } from "@/components/ui/Button";
import { AiActionPanel } from "@/components/ai/AiActionPanel";
import { Input } from "@/components/ui/Input";
import type {
  ApplicationDraftResponse,
  ApplicationDraftSuggestionResponse,
  ApplicationStatus,
  ApplicationTrackingEventResponse,
  MatchAnalysisResponse,
  ResumeResponse,
  VacancyDetail,
} from "@/types/api";

export default function VacancyDetailPage() {
  const params = useParams<{ id: string }>();
  const vacancyId = params.id;
  const { requireToken, session } = useAuth();

  const [vacancy, setVacancy] = useState<VacancyDetail | null>(null);
  const [match, setMatch] = useState<MatchAnalysisResponse | null>(null);
  const [matchState, setMatchState] = useState<MatchLoadState>({ status: "idle" });
  const [applicationDraft, setApplicationDraft] = useState<ApplicationDraftResponse | null>(null);
  const [trackingEvents, setTrackingEvents] = useState<ApplicationTrackingEventResponse[]>([]);
  const [resumes, setResumes] = useState<ResumeResponse[]>([]);
  const [selectedResumeId, setSelectedResumeId] = useState<string>("");
  const [variantLabel, setVariantLabel] = useState("Variant para candidatura");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [statusLoading, setStatusLoading] = useState(false);
  const [variantLoading, setVariantLoading] = useState(false);
  const [draftLoading, setDraftLoading] = useState(false);
  const [matchGenerationLoading, setMatchGenerationLoading] = useState(false);
  const [generatedDraftMessage, setGeneratedDraftMessage] = useState<string>("");

  useEffect(() => {
    let cancelled = false;

    async function loadBase() {
      setLoading(true);
      setError(null);
      setMatch(null);
      setMatchState({ status: "idle" });

      try {
        const token = requireToken();
        const [vacancyData, applicationsPage, resumesPage] = await Promise.all([
          vacanciesApi.byId(token, vacancyId),
          applicationsApi.list(token, 0, 50),
          resumesApi.list(token, 0, 20),
        ]);

        if (cancelled) {
          return;
        }

        setVacancy(vacancyData);
        setApplicationDraft(applicationsPage.items.find((item) => item.vacancyId === vacancyId) ?? null);
        setResumes(resumesPage.items);
        setSelectedResumeId((previous) => previous || resumesPage.items[0]?.id || "");
        setLoading(false);

        await loadMatch(token);
      } catch {
        if (!cancelled) {
          setError("Falha ao carregar detalhe da vaga.");
          setLoading(false);
        }
      }
    }

    async function loadMatch(token: string) {
      setMatchState({ status: "loading" });
      try {
        const scopeKey = session?.user.id ?? "anonymous";
        const next = await getMatchForVacancy(token, vacancyId, {
          scopeKey,
          retry429: 1,
        });
        if (cancelled) {
          return;
        }
        const state = mapMatchResponseToState(next);
        setMatch(state.status === "success" ? state.data : null);
        setMatchState(state);
      } catch (e) {
        if (cancelled) {
          return;
        }
        setMatch(null);
        setMatchState(mapMatchErrorToState(e));
      }
    }

    if (vacancyId) {
      void loadBase();
    }

    return () => {
      cancelled = true;
    };
  }, [requireToken, session?.user.id, vacancyId]);

  async function retryMatch() {
    try {
      const token = requireToken();
      setMatchState({ status: "loading" });
      const scopeKey = session?.user.id ?? "anonymous";
      const next = await getMatchForVacancy(token, vacancyId, { scopeKey, forceRefresh: true, retry429: 1 });
      const state = mapMatchResponseToState(next);
      setMatch(state.status === "success" ? state.data : null);
      setMatchState(state);
    } catch (e) {
      setMatch(null);
      setMatchState(mapMatchErrorToState(e));
    }
  }

  async function generateMatch() {
    setMatchGenerationLoading(true);
    setError(null);
    try {
      const token = requireToken();
      await matchingApi.generate(token, {
        vacancyId,
        resumeId: selectedResumeId || undefined,
      });
      await retryMatch();
    } catch {
      setError("Falha ao gerar match para esta vaga.");
    } finally {
      setMatchGenerationLoading(false);
    }
  }

  async function loadTracking(draftId: string) {
    try {
      const timeline = await applicationsApi.getTracking(requireToken(), draftId);
      setTrackingEvents(timeline);
    } catch {
      setTrackingEvents([]);
    }
  }

  useEffect(() => {
    if (!applicationDraft) {
      setTrackingEvents([]);
      return;
    }
    void loadTracking(applicationDraft.id);
  }, [applicationDraft?.id, requireToken]);

  async function updateStatus(status: ApplicationStatus) {
    if (!applicationDraft) {
      return;
    }
    setStatusLoading(true);
    try {
      const notes =
        status === "READY_FOR_REVIEW"
          ? "manual-review-ready"
          : status === "APPLIED"
          ? "manual-apply-confirmed"
          : status === "WITHDRAWN"
          ? "manual-withdrawn"
          : undefined;
      const updated = await applicationsApi.updateStatus(requireToken(), applicationDraft.id, status, notes);
      setApplicationDraft(updated);
      await loadTracking(updated.id);
    } catch {
      setError("Falha ao atualizar status da candidatura.");
    } finally {
      setStatusLoading(false);
    }
  }

  async function createVariant() {
    if (!selectedResumeId) {
      setError("Selecione um curriculo para criar a variante.");
      return;
    }
    setVariantLoading(true);
    setError(null);
    try {
      await resumesApi.createVariant(requireToken(), selectedResumeId, {
        vacancyId,
        variantLabel: variantLabel.trim() || undefined,
      });
      await retryMatch();
    } catch {
      setError("Falha ao criar variante para esta vaga.");
    } finally {
      setVariantLoading(false);
    }
  }

  async function createApplicationDraftAssisted() {
    setDraftLoading(true);
    setError(null);
    try {
      const draft = await applicationsApi.createDraftAssisted(requireToken(), {
        vacancyId,
        resumeId: selectedResumeId || undefined,
        messageDraft: generatedDraftMessage || undefined,
      });
      setApplicationDraft(draft);
      await loadTracking(draft.id);
    } catch {
      setError("Falha ao criar draft assistido para esta vaga.");
    } finally {
      setDraftLoading(false);
    }
  }

  const orderedBreakdown = useMemo(() => {
    if (!match) return [];
    return Object.entries(match.scoreBreakdown).sort((a, b) => b[1] - a[1]);
  }, [match]);

  if (loading) return <LoadingState label="Carregando detalhe da vaga..." />;
  if (error && !vacancy) return <ErrorState message={error} />;
  if (!vacancy) return <ErrorState message="Dados da vaga indisponiveis." />;

  const normalizedSeniority = vacancy.seniority?.trim() || "Nao informado";
  const canMarkReady = applicationDraft?.status === "DRAFT";
  const canMarkApplied = applicationDraft?.status === "READY_FOR_REVIEW";
  const canWithdraw = applicationDraft ? applicationDraft.status !== "WITHDRAWN" : false;
  const scoreLabel =
    matchState.status === "loading"
      ? "Carregando..."
      : match
      ? String(match.score)
      : matchState.status === "resume_missing"
      ? "Curriculo ausente"
      : matchState.status === "variant_missing"
      ? "Variante ausente"
      : matchState.status === "not_generated"
      ? "Nao gerado"
      : "Indisponivel";

  return (
    <div className="space-y-4">
      <header className="space-y-2">
        <h2 className="text-2xl font-bold">{vacancy.title}</h2>
        <p className="text-sm text-slate-600">
          {vacancy.company} · {vacancy.location || "Local nao informado"}
        </p>
        <div className="flex items-center gap-2">
          {match ? <RecommendationBadge recommendation={match.recommendation} /> : null}
          <span className="text-sm font-semibold text-slate-900">Score deterministico: {scoreLabel}</span>
        </div>
      </header>

      {error ? <ErrorState message={error} /> : null}

      <Card className="space-y-3 p-4">
        <h3 className="text-lg font-semibold">Proximo passo recomendado</h3>
        {matchState.status === "resume_missing" ? (
          <div className="space-y-2 rounded-lg border border-amber-200 bg-amber-50 p-3 text-sm text-amber-900">
            <p>Envie seu curriculo base em PDF para habilitar analise de match nesta vaga.</p>
            <Link href="/curriculos" className="font-semibold text-amber-900 underline">
              Ir para area de curriculos
            </Link>
          </div>
        ) : null}

        {matchState.status === "variant_missing" ? (
          <div className="space-y-3 rounded-lg border border-slate-200 bg-slate-50 p-3 text-sm text-slate-700">
            <p>Esta vaga ainda nao possui variante de curriculo associada.</p>
            {resumes.length === 0 ? (
              <p>
                Nenhum curriculo disponivel para criar variante.{" "}
                <Link href="/curriculos" className="font-semibold text-teal-700 hover:text-teal-600">
                  Registrar curriculo base
                </Link>
              </p>
            ) : (
              <>
                <div className="grid grid-cols-1 gap-2 md:grid-cols-2">
                  <select
                    className="rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm"
                    value={selectedResumeId}
                    onChange={(event) => setSelectedResumeId(event.target.value)}
                  >
                    {resumes.map((resume) => (
                      <option key={resume.id} value={resume.id}>
                        {resume.title} ({resume.status}){resume.base ? " - base" : ""}
                      </option>
                    ))}
                  </select>
                  <Input
                    value={variantLabel}
                    onChange={(event) => setVariantLabel(event.target.value)}
                    maxLength={120}
                    placeholder="Nome da variante"
                  />
                </div>
                <Button type="button" onClick={() => void createVariant()} disabled={variantLoading}>
                  {variantLoading ? "Criando variante..." : "Criar variante para esta vaga"}
                </Button>
              </>
            )}
          </div>
        ) : null}

        {matchState.status === "not_generated" ? (
          <div className="space-y-2 rounded-lg border border-slate-200 bg-slate-50 p-3 text-sm text-slate-700">
            <p>{matchState.message}</p>
            <Button type="button" onClick={() => void generateMatch()} disabled={matchGenerationLoading}>
              {matchGenerationLoading ? "Gerando match..." : "Gerar match agora"}
            </Button>
          </div>
        ) : null}

        {matchState.status === "rate_limited" ? (
          <div className="rounded-lg border border-amber-200 bg-amber-50 p-3 text-sm text-amber-800">
            <p>{matchState.message}</p>
            <Button type="button" className="mt-2" variant="ghost" onClick={() => void retryMatch()}>
              Tentar novamente
            </Button>
          </div>
        ) : null}

        {matchState.status === "error" ? (
          <div className="rounded-lg border border-rose-200 bg-rose-50 p-3 text-sm text-rose-700">
            <p>{matchState.message}</p>
            <Button type="button" className="mt-2" variant="ghost" onClick={() => void retryMatch()}>
              Recarregar analise
            </Button>
          </div>
        ) : null}

        {matchState.status === "not_found" ? (
          <div className="rounded-lg border border-slate-200 bg-slate-50 p-3 text-sm text-slate-700">
            <p>{matchState.message}</p>
            <Button type="button" className="mt-2" variant="ghost" onClick={() => void retryMatch()}>
              Revalidar
            </Button>
          </div>
        ) : null}
      </Card>

      <section className="grid grid-cols-1 gap-4 xl:grid-cols-2">
        <Card className="space-y-3 p-4">
          <h3 className="text-lg font-semibold">Breakdown e explicabilidade</h3>
          {match ? (
            <>
              <div className="space-y-2">
                {orderedBreakdown.map(([criterion, value]) => (
                  <div key={criterion} className="flex items-center justify-between rounded-lg bg-slate-50 px-3 py-2 text-sm">
                    <span>{criterion}</span>
                    <span className="font-semibold">{value}</span>
                  </div>
                ))}
              </div>
              <div>
                <p className="text-sm font-semibold text-slate-800">Strengths</p>
                <ul className="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-700">
                  {match.strengths.length ? match.strengths.map((item) => <li key={item}>{item}</li>) : <li>Sem strengths relevantes.</li>}
                </ul>
              </div>
              <div>
                <p className="text-sm font-semibold text-slate-800">Gaps</p>
                <ul className="mt-1 list-disc space-y-1 pl-5 text-sm text-slate-700">
                  {match.gaps.map((item) => (
                    <li key={item}>{item}</li>
                  ))}
                </ul>
              </div>
            </>
          ) : (
            <p className="text-sm text-slate-600">
              O breakdown fica disponivel apos o match. Use o bloco de proximo passo para destravar.
            </p>
          )}
        </Card>

        <Card className="space-y-3 p-4">
          <h3 className="text-lg font-semibold">Preparacao de candidatura</h3>
          <p className="text-sm text-slate-700">Status da vaga: {vacancy.status}</p>
          <p className="text-sm text-slate-700">Senioridade: {normalizedSeniority}</p>
          <p className="text-sm text-slate-700">
            Data de publicacao: {vacancy.publishedAt ? new Date(vacancy.publishedAt).toLocaleDateString("pt-BR") : "Nao informada"}
          </p>
          <p className="text-sm text-slate-700">Remoto: {vacancy.remote ? "Sim" : "Nao"}</p>

          <div>
            <p className="text-sm font-semibold text-slate-800">Skills exigidas</p>
            <div className="mt-2 flex flex-wrap gap-2">
              {vacancy.requiredSkills.map((skill) => (
                <span key={skill} className="rounded-full bg-slate-100 px-2 py-1 text-xs text-slate-700">
                  {skill}
                </span>
              ))}
            </div>
          </div>

          <div className="rounded-lg border border-slate-200 bg-slate-50 p-3 text-sm text-slate-700">
            {applicationDraft ? (
              <>
                <p className="font-semibold">Candidatura vinculada</p>
                <p>Status atual: {applicationDraft.status}</p>
                <div className="mt-2 flex gap-2">
                  {canMarkReady ? (
                    <Button type="button" onClick={() => void updateStatus("READY_FOR_REVIEW")} disabled={statusLoading}>
                      Pronta para revisão
                    </Button>
                  ) : null}
                  {canMarkApplied ? (
                    <Button type="button" onClick={() => void updateStatus("APPLIED")} disabled={statusLoading}>
                      Confirmar aplicação manual
                    </Button>
                  ) : null}
                  {canWithdraw ? (
                    <Button type="button" variant="ghost" onClick={() => void updateStatus("WITHDRAWN")} disabled={statusLoading}>
                      Marcar como ignorada
                    </Button>
                  ) : null}
                </div>
                {trackingEvents.length ? (
                  <div className="mt-3 space-y-2">
                    <p className="text-xs font-semibold uppercase text-slate-500">Timeline</p>
                    {trackingEvents.map((event) => (
                      <div key={event.id} className="rounded-lg border border-slate-200 bg-white px-3 py-2">
                        <p className="text-xs font-semibold text-slate-700">{event.stage}</p>
                        <p className="text-xs text-slate-500">
                          {new Date(event.createdAt).toLocaleString("pt-BR")}
                          {event.notes ? ` - ${event.notes}` : ""}
                        </p>
                      </div>
                    ))}
                  </div>
                ) : null}
              </>
            ) : (
              <>
                <p>Sem draft de candidatura para esta vaga.</p>
                <Button type="button" className="mt-2" onClick={() => void createApplicationDraftAssisted()} disabled={draftLoading || resumes.length === 0}>
                  {draftLoading ? "Criando draft..." : "Criar draft assistido"}
                </Button>
                {resumes.length === 0 ? (
                  <p className="mt-2 text-xs text-slate-500">
                    Envie um curriculo base em PDF para criar draft assistido.
                  </p>
                ) : null}
              </>
            )}
          </div>

          <div className="rounded-lg border border-slate-200 bg-slate-50 p-3 text-sm text-slate-700">
            <p className="font-semibold">Aplicacao manual na fonte original</p>
            {vacancy.jobUrl ? (
              <a
                href={vacancy.jobUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="mt-2 inline-block text-sm font-semibold text-teal-700 hover:text-teal-600"
              >
                Abrir vaga original
              </a>
            ) : (
              <p className="mt-1 text-xs text-slate-500">
                Link original nao disponivel para esta vaga.
              </p>
            )}
          </div>
        </Card>
      </section>

      {featureFlags.aiActionsEnabled ? (
        <AiActionPanel
          vacancyId={vacancyId}
          onDraftGenerated={(result: ApplicationDraftSuggestionResponse) => setGeneratedDraftMessage(result.shortMessage)}
        />
      ) : null}
    </div>
  );
}
