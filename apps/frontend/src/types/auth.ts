import type { UserRole } from "@/types/api";

export type AuthSession = {
  accessToken: string;
  tokenType: string;
  expiresAtEpochMs: number;
  user: {
    id: string;
    email: string;
    role: UserRole;
  };
};

