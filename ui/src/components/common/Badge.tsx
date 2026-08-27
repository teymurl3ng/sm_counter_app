import React from 'react';

interface BadgeProps {
  children: React.ReactNode;
  variant?: 'emerald' | 'amber' | 'rose' | 'indigo' | 'neutral' | 'sky';
  size?: 'sm' | 'md';
}

const variantStyles = {
  emerald: 'bg-emerald-100 text-emerald-800 border-emerald-200',
  amber: 'bg-amber-100 text-amber-900 border-amber-200',
  rose: 'bg-rose-100 text-rose-800 border-rose-200',
  indigo: 'bg-indigo-100 text-indigo-800 border-indigo-200',
  sky: 'bg-sky-100 text-sky-800 border-sky-200',
  neutral: 'bg-slate-100 text-slate-700 border-slate-200',
};

export const Badge: React.FC<BadgeProps> = ({
  children,
  variant = 'neutral',
  size = 'sm',
}) => {
  return (
    <span
      className={`inline-flex items-center gap-1 font-medium border rounded-md whitespace-nowrap ${
        size === 'sm' ? 'text-[11px] px-2 py-0.5' : 'text-xs px-2.5 py-1'
      } ${variantStyles[variant]}`}
    >
      {children}
    </span>
  );
};
