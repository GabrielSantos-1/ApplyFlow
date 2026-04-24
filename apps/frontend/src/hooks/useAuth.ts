"use client";

import { useAuthContext } from "@/components/layout/AuthProvider";

export function useAuth() {
  return useAuthContext();
}

