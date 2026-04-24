const asBoolean = (value: string | undefined): boolean => value === "true";

export const featureFlags = {
  aiActionsEnabled: asBoolean(process.env.NEXT_PUBLIC_AI_ACTIONS_ENABLED),
} as const;

