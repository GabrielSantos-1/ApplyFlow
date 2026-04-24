import type { AuthSession } from "@/types/auth";

let inMemorySession: AuthSession | null = null;

export function getStoredSession(): AuthSession | null {
  return inMemorySession;
}

export function storeSession(session: AuthSession): void {
  inMemorySession = session;
}

export function clearStoredSession(): void {
  inMemorySession = null;
}

export function isSessionExpired(session: AuthSession): boolean {
  const now = Date.now();
  return session.expiresAtEpochMs <= now + 30_000;
}

