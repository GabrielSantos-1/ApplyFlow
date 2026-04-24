import type { ButtonHTMLAttributes } from "react";

type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: "primary" | "secondary" | "ghost" | "danger";
};

export function Button({ variant = "primary", className = "", ...props }: ButtonProps) {
  const base = "inline-flex items-center justify-center rounded-lg px-4 py-2 text-sm font-semibold transition disabled:cursor-not-allowed disabled:opacity-60";
  const variants: Record<NonNullable<ButtonProps["variant"]>, string> = {
    primary: "bg-slate-900 text-white hover:bg-slate-800",
    secondary: "bg-teal-600 text-white hover:bg-teal-500",
    ghost: "bg-transparent text-slate-700 hover:bg-slate-100",
    danger: "bg-rose-600 text-white hover:bg-rose-500",
  };

  return <button className={`${base} ${variants[variant]} ${className}`} {...props} />;
}

