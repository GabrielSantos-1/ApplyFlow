export type LoginFormInput = {
  email: string;
  password: string;
};

export function validateLoginInput(input: LoginFormInput): string | null {
  if (!input.email.trim() || !input.password.trim()) {
    return "Informe e-mail e senha.";
  }
  if (input.password.length < 8) {
    return "A senha deve ter pelo menos 8 caracteres.";
  }
  return null;
}

