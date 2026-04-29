"use client";

import { useEffect, useMemo, useState } from "react";
import { adminApi } from "@/lib/api/admin";
import { ApiError } from "@/lib/api/client";
import { useAuth } from "@/hooks/useAuth";
import { Button } from "@/components/ui/Button";
import { ErrorState } from "@/components/ui/ErrorState";
import { LoadingState } from "@/components/ui/LoadingState";
import type { AdminIngestionOverviewResponse, AdminProviderOverview } from "@/types/api";

export default function AdminDashboardPage() {
  const { requireToken, session } = useAuth();
  const [overview, setOverview] = useState<AdminIngestionOverviewResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [lastUpdatedAt, setLastUpdatedAt] = useState<string | null>(null);

  async function loadOverview(mode: "initial" | "refresh" = "initial") {
    if (session?.user.role !== "ADMIN") {
      setOverview(null);
      setError("Acesso restrito ao administrador.");
      setLoading(false);
      return;
    }

    if (mode === "initial") {
      setLoading(true);
    } else {
      setRefreshing(true);
    }
    setError(null);

    try {
      const data = await adminApi.ingestionOverview(requireToken());
      setOverview(data);
      setLastUpdatedAt(new Date().toISOString());
    } catch (failure) {
      setError(mapAdminError(failure));
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }

  useEffect(() => {
    void loadOverview("initial");
    // loadOverview intentionally stays local to keep token/role handling explicit in this page.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [session?.user.role]);

  const alerts = useMemo(() => buildOperationalAlerts(overview), [overview]);
  const hasOperationalData = Boolean(overview && (overview.providers.length > 0 || overview.totals.vacanciesTotal > 0));

  return (
    <div className="mx-auto flex w-full max-w-6xl flex-col gap-5 px-1 pb-10 md:px-2">
      <header className="overflow-hidden rounded-3xl border border-slate-200 bg-white shadow-sm">
        <div className="border-b border-slate-100 bg-[radial-gradient(circle_at_top_left,_rgba(20,184,166,0.16),_transparent_34%),linear-gradient(135deg,_#ffffff,_#f8fafc)] px-5 py-6 md:px-8 md:py-8">
          <div className="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
            <div className="max-w-3xl">
              <p className="text-xs font-bold uppercase tracking-[0.22em] text-teal-700">Operação admin</p>
              <h2 className="mt-3 text-3xl font-bold tracking-tight text-slate-950 md:text-4xl">Admin Dashboard</h2>
              <p className="mt-3 text-sm leading-6 text-slate-600 md:text-base">
                Visão operacional da ingestão, qualidade de dados e saúde dos providers. Os dados exibidos são agregados e não incluem payload bruto.
              </p>
            </div>

            <div className="flex flex-col gap-2 rounded-2xl border border-white/70 bg-white/75 p-3 shadow-sm backdrop-blur sm:flex-row sm:items-center">
              <div className="text-sm text-slate-600">
                <p className="font-semibold text-slate-900">Última atualização</p>
                <p>{lastUpdatedAt ? formatDateTime(lastUpdatedAt) : "Ainda não carregado"}</p>
              </div>
              <Button type="button" variant="secondary" onClick={() => void loadOverview("refresh")} disabled={loading || refreshing}>
                {refreshing ? "Atualizando..." : "Atualizar"}
              </Button>
            </div>
          </div>
        </div>
      </header>

      {loading ? <LoadingState label="Carregando visão operacional..." /> : null}
      {error ? <ErrorState message={error} /> : null}

      {!loading && !error && overview && !hasOperationalData ? (
        <section className="rounded-3xl border border-dashed border-slate-300 bg-white p-8 text-center shadow-sm">
          <p className="text-base font-bold text-slate-900">Nenhum dado operacional disponível ainda.</p>
          <p className="mt-2 text-sm text-slate-600">Execute uma ingestão para popular o painel.</p>
        </section>
      ) : null}

      {!loading && !error && overview && hasOperationalData ? (
        <>
          <section className="grid grid-cols-1 gap-3 md:grid-cols-2 xl:grid-cols-4" aria-label="Métricas principais de ingestão">
            <MetricCard title="Total de vagas" value={overview.totals.vacanciesTotal} subtitle="Volume total agregado no repositório" />
            <MetricCard title="Vagas visíveis" value={overview.totals.vacanciesVisible} subtitle="Total após exclusão de duplicadas" tone="success" />
            <MetricCard title="Duplicadas" value={overview.dedupe.duplicateVacancies} subtitle={`${formatPercent(overview.dedupe.duplicateRatePercent)} de dedupe`} tone={overview.dedupe.duplicateRatePercent > 10 ? "warning" : "neutral"} />
            <MetricCard title="Providers ativos" value={`${overview.totals.activeProviders}/${overview.totals.providers}`} subtitle="Fontes configuradas para ingestão" />
            <MetricCard title="Coletadas" value={overview.totals.vacanciesCollected} subtitle="Soma das vagas retornadas pelos providers" />
            <MetricCard title="Persistidas" value={overview.totals.vacanciesPersisted} subtitle="Inseridas ou atualizadas após normalização" tone="success" />
            <MetricCard title="Últimas 24h" value={overview.recent.last24h} subtitle="Vagas criadas recentemente" />
            <MetricCard title="Qualidade média" value={formatNumber(overview.quality.averageQualityScore)} subtitle="Score médio agregado de qualidade" tone={overview.quality.averageQualityScore < 60 ? "warning" : "neutral"} />
          </section>

          <section className="grid grid-cols-1 gap-5 xl:grid-cols-[minmax(0,1.45fr)_minmax(320px,0.75fr)]">
            <ProvidersPanel providers={overview.providers} />
            <QualityPanel overview={overview} alerts={alerts} />
          </section>

          <RecentExecutionsPanel providers={overview.providers} />
        </>
      ) : null}
    </div>
  );
}

function ProvidersPanel({ providers }: { providers: AdminProviderOverview[] }) {
  return (
    <section className="rounded-3xl border border-slate-200 bg-white p-4 shadow-sm md:p-5" aria-labelledby="providers-title">
      <div className="mb-4 border-b border-slate-100 pb-4">
        <h3 id="providers-title" className="text-base font-bold text-slate-950">Saúde dos providers</h3>
        <p className="text-sm text-slate-500">Status e aproveitamento das fontes configuradas.</p>
      </div>

      {providers.length === 0 ? (
        <p className="rounded-2xl border border-dashed border-slate-300 bg-slate-50 p-5 text-sm text-slate-600">Nenhum provider configurado.</p>
      ) : (
        <div className="space-y-3">
          {providers.map((provider) => (
            <article key={provider.sourceConfigId} className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
              <div className="flex flex-col gap-3 lg:flex-row lg:items-start lg:justify-between">
                <div>
                  <div className="flex flex-wrap items-center gap-2">
                    <h4 className="text-sm font-bold text-slate-950">{provider.name}</h4>
                    <StatusBadge active={provider.active} />
                    <span className="rounded-full bg-white px-2.5 py-1 text-xs font-semibold text-slate-600">{provider.sourceType}</span>
                  </div>
                  <p className="mt-2 text-xs text-slate-500">Tenant: {provider.tenant || "não informado"}</p>
                  <p className="mt-1 text-xs text-slate-500">Última execução: {provider.lastExecution ? formatDateTime(provider.lastExecution.startedAt) : "sem execução"}</p>
                </div>

                <div className="grid grid-cols-2 gap-2 text-sm sm:grid-cols-4 lg:min-w-[360px]">
                  <MiniStat label="Coletadas" value={provider.vacanciesCollected} />
                  <MiniStat label="Persistidas" value={provider.vacanciesPersisted} />
                  <MiniStat label="Duplicadas" value={provider.duplicateVacancies} />
                  <MiniStat label="Qualidade" value={formatNumber(provider.averageQualityScore)} />
                </div>
              </div>
            </article>
          ))}
        </div>
      )}
    </section>
  );
}

function QualityPanel({ overview, alerts }: { overview: AdminIngestionOverviewResponse; alerts: string[] }) {
  return (
    <section className="rounded-3xl border border-slate-200 bg-white p-4 shadow-sm md:p-5" aria-labelledby="quality-title">
      <div className="mb-4 border-b border-slate-100 pb-4">
        <h3 id="quality-title" className="text-base font-bold text-slate-950">Qualidade e dedupe</h3>
        <p className="text-sm text-slate-500">Sinais agregados para operação e debugging.</p>
      </div>

      <div className="grid grid-cols-2 gap-3">
        <MiniStat label="Total" value={overview.dedupe.totalVacancies} />
        <MiniStat label="Duplicadas" value={overview.dedupe.duplicateVacancies} />
        <MiniStat label="Dedupe" value={formatPercent(overview.dedupe.duplicateRatePercent)} />
        <MiniStat label="7 dias" value={overview.recent.last7d} />
      </div>

      <div className="mt-5">
        <h4 className="text-sm font-bold text-slate-900">Top quality flags</h4>
        {overview.quality.topFlags.length === 0 ? (
          <p className="mt-2 rounded-2xl border border-dashed border-slate-300 bg-slate-50 p-4 text-sm text-slate-600">Nenhuma flag de qualidade agregada.</p>
        ) : (
          <ul className="mt-3 space-y-2">
            {overview.quality.topFlags.map((flag) => (
              <li key={flag.flag} className="rounded-2xl border border-slate-200 bg-slate-50 p-3">
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <p className="text-sm font-semibold text-slate-900">{friendlyQualityFlag(flag.flag)}</p>
                    <p className="mt-1 font-mono text-xs text-slate-500">{flag.flag}</p>
                  </div>
                  <span className="rounded-full bg-white px-2.5 py-1 text-xs font-bold text-slate-700">{formatNumber(flag.count)}</span>
                </div>
              </li>
            ))}
          </ul>
        )}
      </div>

      <div className="mt-5">
        <h4 className="text-sm font-bold text-slate-900">Alertas operacionais</h4>
        {alerts.length === 0 ? (
          <p className="mt-2 rounded-2xl border border-emerald-100 bg-emerald-50 p-4 text-sm text-emerald-800">Nenhum alerta inferido pelos dados agregados disponíveis.</p>
        ) : (
          <ul className="mt-2 space-y-2">
            {alerts.map((alert) => (
              <li key={alert} className="rounded-2xl border border-amber-200 bg-amber-50 p-3 text-sm text-amber-900">{alert}</li>
            ))}
          </ul>
        )}
      </div>
    </section>
  );
}

function RecentExecutionsPanel({ providers }: { providers: AdminProviderOverview[] }) {
  const executions = providers.filter((provider) => provider.lastExecution);

  return (
    <section className="rounded-3xl border border-slate-200 bg-white p-4 shadow-sm md:p-5" aria-labelledby="executions-title">
      <div className="mb-4 border-b border-slate-100 pb-4">
        <h3 id="executions-title" className="text-base font-bold text-slate-950">Últimas execuções de ingestão</h3>
        <p className="text-sm text-slate-500">Resumo seguro das últimas execuções por provider.</p>
      </div>

      {executions.length === 0 ? (
        <p className="rounded-2xl border border-dashed border-slate-300 bg-slate-50 p-5 text-sm text-slate-600">Nenhuma execução recente encontrada.</p>
      ) : (
        <div className="overflow-x-auto">
          <table className="min-w-[760px] w-full border-separate border-spacing-0 text-left text-sm">
            <thead>
              <tr className="text-xs uppercase tracking-[0.14em] text-slate-500">
                <th className="border-b border-slate-200 px-3 py-3 font-bold">Provider</th>
                <th className="border-b border-slate-200 px-3 py-3 font-bold">Status</th>
                <th className="border-b border-slate-200 px-3 py-3 font-bold">Início</th>
                <th className="border-b border-slate-200 px-3 py-3 font-bold">Coletadas</th>
                <th className="border-b border-slate-200 px-3 py-3 font-bold">Persistidas</th>
                <th className="border-b border-slate-200 px-3 py-3 font-bold">Duplicadas</th>
                <th className="border-b border-slate-200 px-3 py-3 font-bold">Falhas</th>
              </tr>
            </thead>
            <tbody>
              {executions.map((provider) => {
                const execution = provider.lastExecution;
                if (!execution) {
                  return null;
                }
                return (
                  <tr key={provider.sourceConfigId} className="text-slate-700">
                    <td className="border-b border-slate-100 px-3 py-3 font-semibold text-slate-900">{provider.name}</td>
                    <td className="border-b border-slate-100 px-3 py-3"><RunStatusBadge status={execution.status} /></td>
                    <td className="border-b border-slate-100 px-3 py-3">{formatDateTime(execution.startedAt)}</td>
                    <td className="border-b border-slate-100 px-3 py-3">{formatNumber(execution.fetchedCount)}</td>
                    <td className="border-b border-slate-100 px-3 py-3">{formatNumber(execution.persistedCount)}</td>
                    <td className="border-b border-slate-100 px-3 py-3">{formatNumber(provider.duplicateVacancies)}</td>
                    <td className="border-b border-slate-100 px-3 py-3">{formatNumber(execution.failedCount)}</td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}

function MetricCard({ title, value, subtitle, tone = "neutral" }: { title: string; value: number | string; subtitle: string; tone?: "neutral" | "success" | "warning" }) {
  const toneClass = tone === "success" ? "border-emerald-200 bg-emerald-50/60" : tone === "warning" ? "border-amber-200 bg-amber-50/70" : "border-slate-200 bg-white";

  return (
    <article className={`rounded-3xl border p-5 shadow-sm ${toneClass}`}>
      <p className="text-xs font-bold uppercase tracking-[0.16em] text-slate-500">{title}</p>
      <p className="mt-3 text-3xl font-bold tracking-tight text-slate-950">{typeof value === "number" ? formatNumber(value) : value}</p>
      <p className="mt-2 text-sm leading-5 text-slate-600">{subtitle}</p>
    </article>
  );
}

function MiniStat({ label, value }: { label: string; value: number | string }) {
  return (
    <div className="rounded-2xl border border-slate-200 bg-white p-3">
      <p className="text-xs font-semibold uppercase tracking-[0.12em] text-slate-500">{label}</p>
      <p className="mt-1 text-base font-bold text-slate-950">{typeof value === "number" ? formatNumber(value) : value}</p>
    </div>
  );
}

function StatusBadge({ active }: { active: boolean }) {
  return (
    <span className={`rounded-full border px-2.5 py-1 text-xs font-bold ${active ? "border-emerald-200 bg-emerald-100 text-emerald-800" : "border-slate-200 bg-slate-100 text-slate-600"}`}>
      {active ? "Ativo" : "Inativo"}
    </span>
  );
}

function RunStatusBadge({ status }: { status: string }) {
  const normalized = status.toUpperCase();
  const success = normalized === "SUCCESS";
  const failed = normalized === "FAIL" || normalized === "FAILED";
  const className = success
    ? "border-emerald-200 bg-emerald-100 text-emerald-800"
    : failed
    ? "border-rose-200 bg-rose-100 text-rose-800"
    : "border-amber-200 bg-amber-100 text-amber-800";

  return <span className={`rounded-full border px-2.5 py-1 text-xs font-bold ${className}`}>{status || "UNKNOWN"}</span>;
}

function buildOperationalAlerts(overview: AdminIngestionOverviewResponse | null): string[] {
  if (!overview) {
    return [];
  }

  const alerts: string[] = [];
  const inactive = overview.providers.filter((provider) => !provider.active);
  if (inactive.length > 0) {
    alerts.push(`${inactive.length} provider${inactive.length === 1 ? "" : "s"} inativo${inactive.length === 1 ? "" : "s"}.`);
  }
  if (overview.recent.last7d === 0 && overview.totals.vacanciesTotal > 0) {
    alerts.push("Nenhum dado recente de ingestão nos últimos 7 dias.");
  }
  if (overview.dedupe.duplicateRatePercent > 10) {
    alerts.push("Taxa de duplicidade acima de 10%; revisar fontes e dedupe cross-source.");
  }
  if (overview.quality.averageQualityScore > 0 && overview.quality.averageQualityScore < 60) {
    alerts.push("Qualidade média abaixo de 60; revisar normalização e flags mais frequentes.");
  }
  if (overview.providers.some((provider) => provider.lastExecution?.failedCount && provider.lastExecution.failedCount > 0)) {
    alerts.push("Há falhas em pelo menos uma última execução de provider.");
  }

  return alerts;
}

function friendlyQualityFlag(flag: string): string {
  const map: Record<string, string> = {
    MISSING_SKILLS: "Skills ausentes",
    MISSING_OR_UNMAPPED_SENIORITY: "Senioridade ausente ou não mapeada",
    MISSING_LOCATION: "Localização ausente",
    MISSING_COMPANY: "Empresa ausente",
    MISSING_JOB_URL: "URL da vaga ausente",
  };
  return map[flag] ?? "Flag de qualidade";
}

function mapAdminError(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.status === 401 || error.status === 403) {
      return "Acesso restrito ao administrador.";
    }
    return "Não foi possível carregar o painel operacional agora. Verifique se o backend está disponível.";
  }
  return "Não foi possível carregar o painel operacional agora. Verifique se o backend está disponível.";
}

function formatDateTime(value: string | null): string {
  if (!value) {
    return "não disponível";
  }
  return new Intl.DateTimeFormat("pt-BR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value));
}

function formatNumber(value: number): string {
  return new Intl.NumberFormat("pt-BR").format(value);
}

function formatPercent(value: number): string {
  return `${new Intl.NumberFormat("pt-BR", { maximumFractionDigits: 2 }).format(value)}%`;
}