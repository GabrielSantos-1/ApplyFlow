"use client";

import { FormEvent, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { Card } from "@/components/ui/Card";
import { Input } from "@/components/ui/Input";
import { Button } from "@/components/ui/Button";
import { useAuth } from "@/hooks/useAuth";
import { validateLoginInput } from "@/lib/validations/auth";

export default function LoginPage() {
  const router = useRouter();
  const { isAuthenticated, login } = useAuth();
  const [redirectPath, setRedirectPath] = useState("/");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (isAuthenticated) {
      router.replace("/");
    }
  }, [isAuthenticated, router]);

  useEffect(() => {
    if (typeof window === "undefined") {
      return;
    }
    const redirect = new URLSearchParams(window.location.search).get("redirect");
    setRedirectPath(redirect || "/");
  }, []);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const validationError = validateLoginInput({ email, password });
    if (validationError) {
      setError(validationError);
      return;
    }

    setLoading(true);
    setError(null);
    try {
      await login({ email, password });
      router.replace(redirectPath);
    } catch {
      setError("Falha de autenticação. Verifique credenciais e disponibilidade do backend.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center p-4">
      <Card className="w-full max-w-md p-6">
        <p className="text-xs font-semibold uppercase tracking-[0.2em] text-teal-700">ApplyFlow</p>
        <h1 className="mt-2 text-2xl font-bold">Entrar na plataforma</h1>
        <p className="mt-1 text-sm text-slate-600">Use sua conta para acessar ranking, análise e ações com IA.</p>

        <form className="mt-5 space-y-3" onSubmit={handleSubmit}>
          <div>
            <label htmlFor="email" className="mb-1 block text-sm font-medium text-slate-700">E-mail</label>
            <Input id="email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} autoComplete="email" />
          </div>

          <div>
            <label htmlFor="password" className="mb-1 block text-sm font-medium text-slate-700">Senha</label>
            <Input
              id="password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              autoComplete="current-password"
            />
          </div>

          {error ? <p className="text-sm text-rose-700">{error}</p> : null}

          <Button type="submit" className="w-full" disabled={loading}>
            {loading ? "Entrando..." : "Entrar"}
          </Button>
        </form>

        <div className="mt-4 rounded-lg border border-slate-200 bg-slate-50 p-3 text-xs text-slate-700">
          Sessao mantida apenas em memoria do navegador para evitar persistencia local de token.
        </div>
      </Card>
    </div>
  );
}

