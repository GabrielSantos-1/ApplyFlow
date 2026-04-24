import { ApiError } from "@/lib/api/client";

export function mapApplicationApiErrorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.status === 400) {
      return "Acao invalida para o estado atual da candidatura.";
    }
    if (error.status === 401) {
      return "Sessao expirada. Faca login novamente.";
    }
    if (error.status === 403) {
      return "Acesso negado para esta operacao.";
    }
    if (error.status === 404) {
      return "Candidatura nao encontrada ou sem permissao de acesso.";
    }
    if (error.status === 500) {
      return "Erro inesperado ao processar a candidatura.";
    }
  }
  return "Falha ao processar a solicitacao.";
}

