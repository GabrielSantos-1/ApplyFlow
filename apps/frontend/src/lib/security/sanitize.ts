export function safeText(value: string | null | undefined): string {
  if (!value) {
    return "";
  }
  return value.replace(/[\u0000-\u001F\u007F]/g, " ").trim();
}

