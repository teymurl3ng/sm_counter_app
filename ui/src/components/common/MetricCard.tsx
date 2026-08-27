import React from 'react';
import { LucideIcon } from 'lucide-react';

interface MetricCardProps {
  id?: string;
  title: string;
  value: string | number;
  subtitle?: string;
  icon: LucideIcon;
  variant?: 'emerald' | 'indigo' | 'amber' | 'rose' | 'neutral' | 'blue';
  action?: React.ReactNode;
}

const variantStyles = {
  emerald: {
    bg: 'bg-emerald-950/20 border-emerald-800/40 text-emerald-300',
    iconBg: 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20',
    valColor: 'text-emerald-300',
  },
  indigo: {
    bg: 'bg-indigo-950/20 border-indigo-800/40 text-indigo-300',
    iconBg: 'bg-indigo-500/10 text-indigo-400 border-indigo-500/20',
    valColor: 'text-indigo-200',
  },
  amber: {
    bg: 'bg-amber-950/20 border-amber-800/40 text-amber-300',
    iconBg: 'bg-amber-500/10 text-amber-400 border-amber-500/20',
    valColor: 'text-amber-200',
  },
  rose: {
    bg: 'bg-rose-950/20 border-rose-800/40 text-rose-300',
    iconBg: 'bg-rose-500/10 text-rose-400 border-rose-500/20',
    valColor: 'text-rose-300',
  },
  blue: {
    bg: 'bg-sky-950/20 border-sky-800/40 text-sky-300',
    iconBg: 'bg-sky-500/10 text-sky-400 border-sky-500/20',
    valColor: 'text-sky-200',
  },
  neutral: {
    bg: 'bg-neutral-900/50 border-neutral-800 text-neutral-300',
    iconBg: 'bg-neutral-800 text-neutral-400 border-neutral-700/50',
    valColor: 'text-neutral-100',
  },
};

export const MetricCard: React.FC<MetricCardProps> = ({
  id,
  title,
  value,
  subtitle,
  icon: Icon,
  variant = 'neutral',
  action
}) => {
  const styles = variantStyles[variant];

  return (
    <div
      id={id}
      className={`rounded-2xl border p-4.5 transition-all duration-200 shadow-sm flex flex-col justify-between ${styles.bg}`}
    >
      <div className="flex items-center justify-between gap-2">
        <span className="text-xs font-medium text-neutral-400 tracking-wide">{title}</span>
        <div className={`p-2 rounded-xl border ${styles.iconBg}`}>
          <Icon className="w-4 h-4" />
        </div>
      </div>

      <div className="mt-3">
        <div className={`text-2xl font-bold tracking-tight ${styles.valColor}`}>
          {value}
        </div>
        {subtitle && (
          <p className="text-xs text-neutral-400 mt-1 font-medium">{subtitle}</p>
        )}
      </div>

      {action && <div className="mt-3 pt-2.5 border-t border-neutral-800/60">{action}</div>}
    </div>
  );
};
