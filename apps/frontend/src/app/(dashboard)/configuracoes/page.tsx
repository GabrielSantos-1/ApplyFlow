"use client";

import { Card } from "@/components/ui/Card";
import { API_BASE_URL } from "@/lib/api/client";
import { useAuth } from "@/hooks/useAuth";

export default function ConfiguracoesPage() {
  const { session } = useAuth();

  return (
    <div className="space-y-4">
      <header>
        <h2 className="text-2xl font-bold">Configurações</h2>
        <p className="text-sm text-slate-600">Visão de sessão e parâmetros não sensíveis do frontend.</p>
      </header>

      <Card className="space-y-2 p-4 text-sm text-slate-700">
        <p><span className="font-semibold">Usuário:</span> {session?.user.email}</p>
        <p><span className="font-semibold">Role:</span> {session?.user.role}</p>
        <p><span className="font-semibold">Backend URL:</span> {API_BASE_URL}</p>
      </Card>

      <Card className="space-y-2 p-4 text-sm text-slate-700">
        <p className="font-semibold">Segurança client-side</p>
        <ul className="list-disc space-y-1 pl-5">
          <li>Rotas privadas protegidas por guarda de sessão.</li>
          <li>Sem renderização HTML bruto de conteúdo externo/IA.</li>
          <li>Token acessível somente em memória no runtime client.</li>
          <li>No refresh, o frontend tenta reidratar sessão via cookie HttpOnly de refresh.</li>
          <li>Autorização real permanece no backend.</li>
        </ul>
      </Card>
    </div>
  );
}

