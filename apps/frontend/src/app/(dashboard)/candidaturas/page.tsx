"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { Card } from "@/components/ui/Card";
import { ErrorState } from "@/components/ui/ErrorState";
import { LoadingState } from "@/components/ui/LoadingState";
import { Button } from "@/components/ui/Button";
import { useAuth } from "@/hooks/useAuth";
import { applicationsApi } from "@/lib/api/applications";
import { vacanciesApi } from "@/lib/api/vacancies";
import { mapApplicationApiErrorMessage } from "@/lib/applications/errors";
import { ApiError } from "@/lib/api/client";
import {
  getPrimaryNextStatus,
  getRecommendedNextStep,
  labelForStatus,
  timelineIndexFromStatus,
  TIMELINE_STEPS,
} from "@/lib/applications/presentation";
import type { ApplicationDraftResponse, VacancyDetail } from "@/types/api";

export default function CandidaturasPage() {
  const { requireToken } = useAuth();
  const [items, setItems] = useState<ApplicationDraftResponse[]>([]);
  const [vacancyById, setVacancyById] = useState<Record<string, VacancyDetail>>({});
  const [busyApplicationId, setBusyApplicationId] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
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
        const page = await applicationsApi.list(token);
        if (cancelled) {
          return;
        }
        setItems(page.items);

        const vacancyIds = [...new Set(page.items.map((item) => item.vacancyId))];
        const detailPairs = await Promise.allSettled(
          vacancyIds.map(async (vacancyId) => {
            const detail = await vacanciesApi.byId(token, vacancyId);
            return [vacancyId, detail] as const;
          })
        );
        if (cancelled) {
          return;
        }
        const nextVacancyById: Record<string, VacancyDetail> = {};
        detailPairs.forEach((result) => {
          if (result.status === "fulfilled") {
            const [vacancyId, detail] = result.value;
            nextVacancyById[vacancyId] = detail;
            return;
          }
          if (result.reason instanceof ApiError && (result.reason.status === 401 || result.reason.status === 403)) {
            throw result.reason;
          }
        });
        setVacancyById(nextVacancyById);
      } catch (e) {
        if (!cancelled) {
          setError(mapApplicationApiErrorMessage(e));
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    void load();
    return () => {
      cancelled = true;
    };
  }, [requireToken]);

  async function changeStatus(id: string) {
    const current = items.find((item) => item.id === id);
    if (!current) {
      return;
    }
    const nextStatus = getPrimaryNextStatus(current.status);
    if (!nextStatus) {
      return;
    }

    setBusyApplicationId(id);
    setFeedback(null);
    setError(null);

    try {
      const updated = await applicationsApi.updateStatus(requireToken(), id, nextStatus);
      setItems((prev) => prev.map((item) => (item.id === id ? updated : item)));
      setFeedback("Status atualizado com sucesso.");
    } catch (e) {
      setError(mapApplicationApiErrorMessage(e));
    } finally {
      setBusyApplicationId(null);
    }
  }

  if (loading) {
    return <LoadingState label="Carregando candidaturas..." />;
  }
  if (error) {
    return <ErrorState message={error} />;
  }

  return (
    <div className="space-y-4">
      <header>
        <h2 className="text-2xl font-bold">Candidaturas</h2>
        <p className="text-sm text-slate-600">Acompanhe etapas da candidatura com linguagem clara e a proxima acao valida.</p>
      </header>

      {feedback ? <div className="rounded-lg border border-emerald-200 bg-emerald-50 p-3 text-sm text-emerald-800">{feedback}</div> : null}

      {items.length === 0 ? (
        <Card className="p-6 text-sm text-slate-600">Nenhuma candidatura registrada ainda.</Card>
      ) : null}

      <div className="space-y-2">
        {items.map((item) => (
          <Card key={item.id} className="space-y-4 p-4">
            <div className="flex flex-wrap items-start justify-between gap-3">
              <div className="space-y-1">
                <p className="text-sm font-semibold text-slate-900">{vacancyById[item.vacancyId]?.title || "Vaga"}</p>
                <p className="text-sm text-slate-600">{vacancyById[item.vacancyId]?.company || "Empresa indisponivel"}</p>
                <p className="text-xs text-slate-500">ID da candidatura: {item.id}</p>
              </div>
              <span className="rounded-full bg-teal-50 px-3 py-1 text-xs font-semibold text-teal-800">{labelForStatus(item.status)}</span>
            </div>

            <div className="space-y-2">
              <div className="flex flex-wrap gap-2">
                {TIMELINE_STEPS.map((stepLabel, index) => (
                  <span
                    key={stepLabel}
                    className={`rounded-full px-2 py-1 text-xs ${
                      index <= timelineIndexFromStatus(item.status) ? "bg-slate-900 text-white" : "bg-slate-100 text-slate-500"
                    }`}
                  >
                    {stepLabel}
                  </span>
                ))}
              </div>
              <p className="text-sm text-slate-600">Proximo passo: {getRecommendedNextStep(item.status)}</p>
            </div>

            <div className="flex flex-wrap items-center gap-2">
              <Link className="text-sm font-semibold text-teal-700 hover:text-teal-600" href={`/candidaturas/${item.id}`}>
                Ver timeline completa
              </Link>
              <Link className="text-xs font-semibold text-slate-600 hover:text-slate-500" href={`/vagas/${item.vacancyId}`}>
                Abrir vaga
              </Link>
              {getPrimaryNextStatus(item.status) ? (
                <Button type="button" variant="secondary" disabled={busyApplicationId === item.id} onClick={() => void changeStatus(item.id)}>
                  {busyApplicationId === item.id ? "Atualizando..." : labelForStatus(getPrimaryNextStatus(item.status)!)}
                </Button>
              ) : (
                <span className="text-xs text-slate-500">Sem acoes pendentes.</span>
              )}
            </div>
          </Card>
        ))}
      </div>
    </div>
  );
}
