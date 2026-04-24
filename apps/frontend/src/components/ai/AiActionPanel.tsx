"use client";

import { useState } from "react";
import { aiApi } from "@/lib/api/ai";
import { ApiError } from "@/lib/api/client";
import { useAuth } from "@/hooks/useAuth";
import { Card } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { ErrorState } from "@/components/ui/ErrorState";
import type {
  ApplicationDraftSuggestionResponse,
  CvImprovementResponse,
  MatchEnrichmentResponse,
} from "@/types/api";

function CopyBlock({ value, label }: { value: string; label: string }) {
  const [copied, setCopied] = useState(false);

  async function copy() {
    await navigator.clipboard.writeText(value);
    setCopied(true);
    window.setTimeout(() => setCopied(false), 1500);
  }

  return (
    <div className="space-y-2 rounded-lg border border-slate-200 bg-slate-50 p-3">
      <p className="text-xs font-semibold uppercase text-slate-600">{label}</p>
      <p className="whitespace-pre-line text-sm text-slate-800">{value}</p>
      <Button type="button" variant="ghost" onClick={copy}>
        {copied ? "Copiado" : "Copiar"}
      </Button>
    </div>
  );
}

type AiActionPanelProps = {
  vacancyId: string;
  onDraftGenerated?: (result: ApplicationDraftSuggestionResponse) => void;
};

export function AiActionPanel({ vacancyId, onDraftGenerated }: AiActionPanelProps) {
  const { requireToken } = useAuth();
  const [loading, setLoading] = useState<"enrichment" | "cv" | "draft" | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [enrichment, setEnrichment] = useState<MatchEnrichmentResponse | null>(null);
  const [cv, setCv] = useState<CvImprovementResponse | null>(null);
  const [draft, setDraft] = useState<ApplicationDraftSuggestionResponse | null>(null);

  const toAiErrorMessage = (fallback: string, cause: unknown) => {
    if (cause instanceof ApiError && cause.status === 404) {
      return "Acoes de IA indisponiveis neste ambiente.";
    }
    return fallback;
  };

  async function runEnrichment() {
    setError(null);
    setLoading("enrichment");
    try {
      const result = await aiApi.enrichMatch(requireToken(), vacancyId);
      setEnrichment(result);
    } catch (cause) {
      setError(toAiErrorMessage("Falha ao gerar enriquecimento de match.", cause));
    } finally {
      setLoading(null);
    }
  }

  async function runCvImprovement() {
    setError(null);
    setLoading("cv");
    try {
      const result = await aiApi.improveCv(requireToken(), vacancyId);
      setCv(result);
    } catch (cause) {
      setError(toAiErrorMessage("Falha ao gerar sugestões de currículo.", cause));
    } finally {
      setLoading(null);
    }
  }

  async function runApplicationDraft() {
    setError(null);
    setLoading("draft");
    try {
      const result = await aiApi.draftApplication(requireToken(), vacancyId);
      setDraft(result);
      onDraftGenerated?.(result);
    } catch (cause) {
      setError(toAiErrorMessage("Falha ao gerar mensagem de candidatura.", cause));
    } finally {
      setLoading(null);
    }
  }

  return (
    <Card className="space-y-4 p-4">
      <div>
        <h3 className="text-lg font-semibold">Ações com IA</h3>
        <p className="text-sm text-slate-600">Camada auxiliar: score e recomendação continuam determinísticos.</p>
      </div>

      <div className="flex flex-wrap gap-2">
        <Button type="button" onClick={runEnrichment} disabled={loading !== null}>
          {loading === "enrichment" ? "Gerando..." : "Enriquecer match"}
        </Button>
        <Button type="button" variant="secondary" onClick={runCvImprovement} disabled={loading !== null}>
          {loading === "cv" ? "Gerando..." : "Melhorar CV"}
        </Button>
        <Button type="button" variant="ghost" onClick={runApplicationDraft} disabled={loading !== null}>
          {loading === "draft" ? "Gerando..." : "Gerar mensagem"}
        </Button>
      </div>

      {error ? <ErrorState message={error} /> : null}

      {enrichment ? (
        <div className="space-y-2">
          <p className="text-sm font-semibold text-slate-800">
            Match enrichment {enrichment.fallbackUsed ? "(fallback)" : "(provider real)"}
          </p>
          <CopyBlock label="Resumo" value={enrichment.summary} />
          <CopyBlock label="Gaps" value={enrichment.gaps.join("\n")} />
          <CopyBlock label="Próximos passos" value={enrichment.nextSteps.join("\n")} />
        </div>
      ) : null}

      {cv ? (
        <div className="space-y-2">
          <p className="text-sm font-semibold text-slate-800">
            CV improvement {cv.fallbackUsed ? "(fallback)" : "(provider real)"}
          </p>
          <CopyBlock label="Sugestões" value={cv.improvementSuggestions.join("\n")} />
          <CopyBlock label="Palavras-chave ATS" value={cv.atsKeywords.join(", ")} />
        </div>
      ) : null}

      {draft ? (
        <div className="space-y-2">
          <p className="text-sm font-semibold text-slate-800">
            Application draft {draft.fallbackUsed ? "(fallback)" : "(provider real)"}
          </p>
          <CopyBlock label="Mensagem curta" value={draft.shortMessage} />
          <CopyBlock label="Mini cover note" value={draft.miniCoverNote} />
        </div>
      ) : null}
    </Card>
  );
}

