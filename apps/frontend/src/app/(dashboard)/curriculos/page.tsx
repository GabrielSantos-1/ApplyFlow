"use client";

import Link from "next/link";
import { FormEvent, useEffect, useMemo, useState } from "react";
import { applicationsApi } from "@/lib/api/applications";
import { resumesApi } from "@/lib/api/resumes";
import { useAuth } from "@/hooks/useAuth";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { ErrorState } from "@/components/ui/ErrorState";
import { Input } from "@/components/ui/Input";
import { LoadingState } from "@/components/ui/LoadingState";
import type { ApplicationDraftResponse, ResumeResponse } from "@/types/api";

export default function CurriculosPage() {
  const { requireToken } = useAuth();
  const [resumes, setResumes] = useState<ResumeResponse[]>([]);
  const [applications, setApplications] = useState<ApplicationDraftResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [uploading, setUploading] = useState(false);
  const [title, setTitle] = useState("Curriculo base");
  const [pdfFile, setPdfFile] = useState<File | null>(null);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      setLoading(true);
      setError(null);
      try {
        const token = requireToken();
        const [resumePage, applicationsPage] = await Promise.all([
          resumesApi.list(token, 0, 20),
          applicationsApi.list(token, 0, 100),
        ]);

        if (cancelled) {
          return;
        }

        setResumes(resumePage.items);
        setApplications(applicationsPage.items);
      } catch {
        if (!cancelled) {
          setError("Falha ao carregar area de curriculos.");
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

  const knownVariants = useMemo(
    () =>
      applications
        .map((item) => ({
          resumeVariantId: item.resumeVariantId,
          vacancyId: item.vacancyId,
          applicationStatus: item.status,
        }))
        .filter(
          (item, index, self) =>
            self.findIndex(
              (candidate) =>
                candidate.resumeVariantId === item.resumeVariantId && candidate.vacancyId === item.vacancyId
            ) === index
        ),
    [applications]
  );

  async function submitResumePdf(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!title.trim()) {
      setError("Informe o titulo do curriculo.");
      return;
    }
    if (!pdfFile) {
      setError("Selecione um arquivo PDF.");
      return;
    }

    setUploading(true);
    setError(null);
    try {
      const created = await resumesApi.uploadPdf(requireToken(), {
        title: title.trim(),
        file: pdfFile,
      });
      setResumes((prev) => [created, ...prev.map((resume) => ({ ...resume, base: false }))]);
      setPdfFile(null);
    } catch {
      setError("Nao foi possivel enviar o curriculo PDF.");
    } finally {
      setUploading(false);
    }
  }

  if (loading) return <LoadingState label="Carregando curriculos..." />;
  if (error && resumes.length === 0) return <ErrorState message={error} />;

  return (
    <div className="space-y-4">
      <header>
        <h2 className="text-2xl font-bold">Curriculos</h2>
        <p className="text-sm text-slate-600">
          Envie curriculo PDF, acompanhe o curriculo base e destrave analise de match por vaga.
        </p>
      </header>

      {error ? <ErrorState message={error} /> : null}

      <Card className="space-y-3 p-4">
        <h3 className="text-lg font-semibold">Upload de curriculo PDF</h3>
        <p className="text-sm text-slate-600">
          O arquivo e armazenado em storage privado com validacao de tipo e checksum no backend.
        </p>
        <form className="grid grid-cols-1 gap-2 md:grid-cols-3" onSubmit={submitResumePdf}>
          <Input
            placeholder="Titulo do curriculo"
            value={title}
            onChange={(event) => setTitle(event.target.value)}
            maxLength={120}
          />
          <Input
            type="file"
            accept="application/pdf,.pdf"
            onChange={(event) => setPdfFile(event.target.files?.[0] ?? null)}
          />
          <Button type="submit" disabled={uploading}>
            {uploading ? "Enviando..." : "Enviar PDF"}
          </Button>
        </form>
      </Card>

      <section className="grid grid-cols-1 gap-4 xl:grid-cols-2">
        <Card className="space-y-3 p-4">
          <h3 className="text-lg font-semibold">Curriculos disponiveis</h3>
          {resumes.length === 0 ? (
            <div className="rounded-lg border border-slate-200 bg-slate-50 p-3 text-sm text-slate-700">
              <p>Voce ainda nao possui curriculo registrado.</p>
              <p className="mt-1 text-slate-500">
                Sem curriculo base, o match deterministico e o fluxo de candidatura assistida ficam indisponiveis.
              </p>
            </div>
          ) : (
            <div className="space-y-2">
              {resumes.map((resume) => (
                <div key={resume.id} className="rounded-lg border border-slate-200 bg-white p-3">
                  <p className="font-semibold text-slate-900">
                    {resume.title} {resume.base ? <span className="text-teal-700">(base)</span> : null}
                  </p>
                  <p className="text-sm text-slate-600">Arquivo: {resume.sourceFileName}</p>
                  <p className="text-xs text-slate-500">Status: {resume.status}</p>
                  <p className="text-xs text-slate-500">Tipo: {resume.contentType ?? "metadata_only"}</p>
                  <p className="text-xs text-slate-500">
                    Tamanho: {resume.fileSizeBytes ? `${Math.round(resume.fileSizeBytes / 1024)} KB` : "nao informado"}
                  </p>
                  <p className="text-xs text-slate-500">
                    Upload: {resume.uploadedAt ? new Date(resume.uploadedAt).toLocaleString("pt-BR") : "nao informado"}
                  </p>
                </div>
              ))}
            </div>
          )}
        </Card>

        <Card className="space-y-3 p-4">
          <h3 className="text-lg font-semibold">Variantes conhecidas (via candidaturas)</h3>
          <p className="text-sm text-slate-600">
            O contrato backend ainda nao possui listagem direta de variantes por curriculo.
          </p>
          {knownVariants.length === 0 ? (
            <div className="rounded-lg border border-slate-200 bg-slate-50 p-3 text-sm text-slate-700">
              Nenhuma variante conhecida ainda. Abra uma vaga para gerar variante e draft assistido.
            </div>
          ) : (
            <div className="space-y-2">
              {knownVariants.map((variant) => (
                <div key={`${variant.resumeVariantId}:${variant.vacancyId}`} className="rounded-lg border border-slate-200 bg-white p-3">
                  <p className="text-sm font-semibold text-slate-900">Variant ID: {variant.resumeVariantId}</p>
                  <p className="text-xs text-slate-600">Vacancy ID: {variant.vacancyId}</p>
                  <p className="text-xs text-slate-500">Status da candidatura: {variant.applicationStatus}</p>
                </div>
              ))}
            </div>
          )}
        </Card>
      </section>

      <Card className="space-y-2 p-4">
        <h3 className="text-base font-semibold">Proximos passos</h3>
        <ul className="list-disc space-y-1 pl-5 text-sm text-slate-700">
          <li>Abra uma vaga e valide se o match esta disponivel.</li>
          <li>Se faltar variante, use o CTA no detalhe para criar.</li>
          <li>Use IA para mensagem e melhorias, depois marque status da candidatura.</li>
        </ul>
        <div className="pt-2">
          <Link href="/vagas" className="text-sm font-semibold text-teal-700 hover:text-teal-600">
            Ir para vagas
          </Link>
        </div>
      </Card>
    </div>
  );
}
