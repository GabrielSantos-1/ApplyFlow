import { Card } from "@/components/ui/Card";

type SummaryCardProps = {
  title: string;
  value: string | number;
  hint?: string;
};

export function SummaryCard({ title, value, hint }: SummaryCardProps) {
  return (
    <Card className="p-4">
      <p className="text-xs font-semibold uppercase tracking-[0.12em] text-slate-500">{title}</p>
      <p className="mt-2 text-2xl font-bold text-slate-900">{value}</p>
      {hint ? <p className="mt-1 text-xs text-slate-600">{hint}</p> : null}
    </Card>
  );
}

