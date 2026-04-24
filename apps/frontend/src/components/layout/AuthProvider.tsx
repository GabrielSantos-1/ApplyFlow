"use client";

import { authApi } from "@/lib/api/auth";
import { clearStoredSession, getStoredSession, isSessionExpired, storeSession } from "@/lib/auth/session";
import type { AuthSession } from "@/types/auth";
import React, { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";

type LoginInput = {
  email: string;
  password: string;
};

type AuthContextValue = {
  isReady: boolean;
  isAuthenticated: boolean;
  session: AuthSession | null;
  login: (input: LoginInput) => Promise<void>;
  logout: () => Promise<void>;
  requireToken: () => string;
};

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [session, setSession] = useState<AuthSession | null>(null);
  const [isReady, setIsReady] = useState(false);

  useEffect(() => {
    let cancelled = false;

    async function bootstrapSession() {
      const stored = getStoredSession();
      if (stored && !isSessionExpired(stored)) {
        if (!cancelled) {
          setSession(stored);
          setIsReady(true);
        }
        return;
      }

      clearStoredSession();
      setSession(null);

      // Reidrata sessao apenas via cookie HttpOnly quando disponivel.
      try {
        const auth = await authApi.refresh();
        const nextSession: AuthSession = {
          accessToken: auth.accessToken,
          tokenType: auth.tokenType,
          expiresAtEpochMs: Date.now() + auth.expiresInSeconds * 1000,
          user: {
            id: auth.userId,
            email: auth.email,
            role: auth.role,
          },
        };
        storeSession(nextSession);
        if (!cancelled) {
          setSession(nextSession);
        }
      } catch {
        clearStoredSession();
        if (!cancelled) {
          setSession(null);
        }
      } finally {
        if (!cancelled) {
          setIsReady(true);
        }
      }
    }

    void bootstrapSession();
    return () => {
      cancelled = true;
    };
  }, []);

  const login = useCallback(async (input: LoginInput) => {
    const auth = await authApi.login(input);
    const nextSession: AuthSession = {
      accessToken: auth.accessToken,
      tokenType: auth.tokenType,
      expiresAtEpochMs: Date.now() + auth.expiresInSeconds * 1000,
      user: {
        id: auth.userId,
        email: auth.email,
        role: auth.role,
      },
    };
    storeSession(nextSession);
    setSession(nextSession);
  }, []);

  const logout = useCallback(async () => {
    try {
      if (session?.accessToken) {
        await authApi.logout(undefined, session.accessToken);
      }
    } catch {
      // Logout local sempre deve ocorrer.
    } finally {
      clearStoredSession();
      setSession(null);
    }
  }, [session?.accessToken]);

  const requireToken = useCallback(() => {
    if (!session || isSessionExpired(session)) {
      clearStoredSession();
      setSession(null);
      throw new Error("SESSION_EXPIRED");
    }
    return session.accessToken;
  }, [session]);

  const value = useMemo<AuthContextValue>(
    () => ({
      isReady,
      isAuthenticated: !!session,
      session,
      login,
      logout,
      requireToken,
    }),
    [isReady, session, login, logout, requireToken]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuthContext(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error("useAuthContext deve ser usado dentro de AuthProvider");
  }
  return ctx;
}

