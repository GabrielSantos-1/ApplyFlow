"use client";

import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import { useParams, usePathname, useRouter } from "next/navigation";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { ErrorState } from "@/components/ui/ErrorState";
import { LoadingState } from "@/components/ui/LoadingState";
import { useAuth } from "@/hooks/useAuth";
import { applicationsApi } from "@/lib/api/applications";
import { vacanciesApi } from "@/lib/api/vacancies";
import { mapApplicationApiErrorMessage } from "@/lib/applications/errors";
import {
  getPrimaryNextStatus,
  getRecommendedNextStep,
  labelForStatus,
  labelForTrackingStage,
  timelineIndexFromStatus,
  TIMELINE_STEPS,
} from "@/lib/applications/presentation";
import type { ApplicationDraftResponse, ApplicationTrackingEventResponse, VacancyDetail } from "@/types/api";

export default function CandidaturaDetailPage() {
  const params = useParams<{ id: string }>();
  const router = useRouter();
  const pathname = usePathname();
  const { requireToken } = useAuth();
  const applicationId = params.id;

  const [application, setApplication] = useState<ApplicationDraftResponse | null>(null);
  const [vacancy, setVacancy] = useState<VacancyDetail | null>(null);
  const [tracking, setTracking] = useState<ApplicationTrackingEventResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [feedback, setFeedback] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      setLoading(true);
      setError(null);
      setFeedback(null);

      try {
        const token = requireToken();
        const draft = await applicationsApi.getById(token, applicationId);
        const trackingData = await applicationsApi.getTracking(token, draft.id);

        let vacancyData: VacancyDetail | null = null;
        try {
          vacancyData = await vacanciesApi.byId(token, draft.vacancyId);
        } catch {
          vacancyData = null;
        }

        if (cancelled) {
          return;
        }
        setApplication(draft);
        setVacancy(vacancyData);
        setTracking(trackingData);
      } catch (e) {
        const message = mapApplicationApiErrorMessage(e);
        if (message.includes("Sessao expirada")) {
          const redirect = pathname ? `?redirect=${encodeURIComponent(pathname)}` : "";
          router.replace(`/login${redirect}`);
          return;
        }
        if (!cancelled) {
          setError(message);
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    if (applicationId) {
      void load();
    }
    return () => {
      cancelled = true;
    };
  }, [applicationId, pathname, requireToken, router]);

  async function runPrimaryTransition() {
    if (!application) {
      return;
    }
    const next = getPrimaryNextStatus(application.status);
    if (!next) {
      return;
    }

    setActionLoading(true);
    setError(null);
    setFeedback(null);

    try {
      const token = requireToken();
      const updated = await applicationsApi.updateStatus(token, application.id, next);
      const timeline = await applicationsApi.getTracking(token, application.id);
      setApplication(updated);
      setTracking(timeline);
      setFeedback("Status atualizado com sucesso.");
    } catch (e) {
      setError(mapApplicationApiErrorMessage(e));
    } finally {
      setActionLoading(false);
    }
  }

  const currentTimelineIndex = useMemo(() => {
    if (!application) {
      return 0;
    }
    return timelineIndexFromStatus(application.status);
  }, [application]);

  if (loading) {
    return <LoadingState label="Carregando timeline da candidatura..." />;
  }
  if (error) {
    return <ErrorState message={error} />;
  }
  if (!application) {
    return <ErrorState message="Candidatura nao encontrada." />;
  }

  const nextStatus = getPrimaryNextStatus(application.status);

  return (
    <div className="space-y-4">
      <header className="space-y-1">
        <h2 className="text-2xl font-bold">Timeline da candidatura</h2>
        <p className="text-sm text-slate-600">{vacancy?.title || "Vaga indisponivel"} {vacancy?.company ? `- ${vacancy.company}` : ""}</p>
        <p className="text-xs text-slate-500">ID: {application.id}</p>
      </header>

      {feedback ? <div className="rounded-lg border border-emerald-200 bg-emerald-50 p-3 text-sm text-emerald-800">{feedback}</div> : null}

      <Card className="space-y-3 p-4">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <p className="text-sm font-semibold text-slate-900">Status atual: {labelForStatus(application.status)}</p>
          <span className="rounded-full bg-slate-900 px-3 py-1 text-xs font-semibold text-white">Etapa atual destacada</span>
        </div>

        <div className="flex flex-wrap gap-2">
          {TIMELINE_STEPS.map((step, index) => (
            <span
              key={step}
              className={`rounded-full px-2 py-1 text-xs ${
                index <= currentTimelineIndex ? "bg-slate-900 text-white" : "bg-slate-100 text-slate-500"
              }`}
            >
              {step}
            </span>
          ))}
        </div>

        <p className="text-sm text-slate-600">Proximo passo recomendado: {getRecommendedNextStep(application.status)}</p>

        {nextStatus ? (
          <Button type="button" variant="secondary" disabled={actionLoading} onClick={() => void runPrimaryTransition()}>
            {actionLoading ? "Atualizando..." : labelForStatus(nextStatus)}
          </Button>
        ) : (
          <p className="text-sm text-slate-500">Nao ha proxima acao valida para este estado.</p>
        )}
      </Card>

      <Card className="space-y-3 p-4">
        <h3 className="text-lg font-semibold">Historico de eventos</h3>
        {tracking.length === 0 ? (
          <p className="text-sm text-slate-600">Sem eventos registrados ate o momento.</p>
        ) : (
          <div className="space-y-2">
            {tracking.map((event, index) => (
              <div
                key={event.id}
                className={`rounded-lg border px-3 py-2 ${
                  index === tracking.length - 1 ? "border-teal-300 bg-teal-50" : "border-slate-200 bg-white"
                }`}
              >
                <p className="text-sm font-semibold text-slate-800">{labelForTrackingStage(event.stage)}</p>
                <p className="text-xs text-slate-500">{new Date(event.createdAt).toLocaleString("pt-BR")}</p>
                {event.notes ? <p className="mt-1 text-sm text-slate-700">{event.notes}</p> : null}
              </div>
            ))}
          </div>
        )}
      </Card>

      <div className="flex flex-wrap gap-2">
        <Link className="text-sm font-semibold text-teal-700 hover:text-teal-600" href="/candidaturas">
          Voltar para candidaturas
        </Link>
        {vacancy ? (
          <Link className="text-sm font-semibold text-slate-700 hover:text-slate-600" href={`/vagas/${vacancy.id}`}>
            Abrir vaga
          </Link>
        ) : null}
      </div>
    </div>
  );
}
