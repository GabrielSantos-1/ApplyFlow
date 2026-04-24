"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { Button } from "@/components/ui/Button";
import { useAuth } from "@/hooks/useAuth";

const NAV_ITEMS = [
  { href: "/", label: "Dashboard" },
  { href: "/curriculos", label: "Currículos" },
  { href: "/vagas", label: "Vagas" },
  { href: "/ranking", label: "Ranking" },
  { href: "/candidaturas", label: "Candidaturas" },
  { href: "/configuracoes", label: "Configurações" },
];

export function AppShell({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const { session, logout } = useAuth();

  async function onLogout() {
    await logout();
    router.replace("/login");
  }

  return (
    <div className="min-h-screen bg-slate-100 text-slate-900">
      <div className="mx-auto flex min-h-screen w-full max-w-[1440px] flex-col lg:flex-row">
        <aside className="border-b border-slate-200 bg-white px-4 py-5 lg:w-72 lg:border-b-0 lg:border-r">
          <div className="mb-6">
            <p className="text-xs font-semibold uppercase tracking-[0.18em] text-teal-700">ApplyFlow</p>
            <h1 className="mt-1 text-xl font-bold">Job Copilot</h1>
            <p className="mt-1 text-xs text-slate-500">Produto orientado a decisão</p>
          </div>

          <nav className="space-y-1">
            {NAV_ITEMS.map((item) => {
              const active = pathname === item.href;
              return (
                <Link
                  key={item.href}
                  href={item.href}
                  className={`block rounded-lg px-3 py-2 text-sm font-medium transition ${
                    active ? "bg-slate-900 text-white" : "text-slate-700 hover:bg-slate-100"
                  }`}
                >
                  {item.label}
                </Link>
              );
            })}
          </nav>

          <div className="mt-6 rounded-lg border border-slate-200 bg-slate-50 p-3 text-xs text-slate-600">
            <p className="font-semibold text-slate-800">Sessão ativa</p>
            <p className="mt-1 break-all">{session?.user.email}</p>
            <p>Role: {session?.user.role}</p>
          </div>

          <Button className="mt-4 w-full" variant="ghost" onClick={onLogout}>
            Sair
          </Button>
        </aside>

        <main className="flex-1 p-4 md:p-6">{children}</main>
      </div>
    </div>
  );
}

