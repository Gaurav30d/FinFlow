import React from 'react';
import { motion } from 'framer-motion';
import { ArrowDownLeft, ArrowUpRight, Scale, TrendingUp, TrendingDown } from 'lucide-react';
import { formatCurrency } from '../utils/formatters';

interface FinancialMetricCardProps {
  type: 'inflow' | 'outflow' | 'net';
  amount: number;
  label: string;
  subtitle?: string;
  isLoading?: boolean;
}

export const FinancialMetricCard: React.FC<FinancialMetricCardProps> = ({
  type,
  amount,
  label,
  subtitle,
  isLoading = false,
}) => {
  const getCardConfig = () => {
    switch (type) {
      case 'inflow':
        return {
          icon: ArrowDownLeft,
          iconBg: 'bg-emerald-50 dark:bg-emerald-950/40 text-emerald-600 dark:text-emerald-400 border-emerald-100 dark:border-emerald-900/40',
          badgeBg: 'bg-emerald-50 dark:bg-emerald-950/30 text-emerald-700 dark:text-emerald-400 border-emerald-200 dark:border-emerald-800/40',
          trendIcon: TrendingUp,
          accentColor: 'text-emerald-600 dark:text-emerald-400',
        };
      case 'outflow':
        return {
          icon: ArrowUpRight,
          iconBg: 'bg-rose-50 dark:bg-rose-950/40 text-rose-600 dark:text-rose-400 border-rose-100 dark:border-rose-900/40',
          badgeBg: 'bg-rose-50 dark:bg-rose-950/30 text-rose-700 dark:text-rose-400 border-rose-200 dark:border-rose-800/40',
          trendIcon: TrendingDown,
          accentColor: 'text-rose-600 dark:text-rose-400',
        };
      case 'net':
      default:
        const isPositive = amount >= 0;
        return {
          icon: Scale,
          iconBg: isPositive
            ? 'bg-blue-50 dark:bg-blue-950/40 text-blue-600 dark:text-blue-400 border-blue-100 dark:border-blue-900/40'
            : 'bg-amber-50 dark:bg-amber-950/40 text-amber-600 dark:text-amber-400 border-amber-100 dark:border-amber-900/40',
          badgeBg: isPositive
            ? 'bg-blue-50 dark:bg-blue-950/30 text-blue-700 dark:text-blue-400 border-blue-200 dark:border-blue-800/40'
            : 'bg-amber-50 dark:bg-amber-950/30 text-amber-700 dark:text-amber-400 border-amber-200 dark:border-amber-800/40',
          trendIcon: isPositive ? TrendingUp : TrendingDown,
          accentColor: isPositive ? 'text-blue-600 dark:text-blue-400' : 'text-amber-600 dark:text-amber-400',
        };
    }
  };

  const config = getCardConfig();
  const Icon = config.icon;

  return (
    <motion.div
      whileHover={{ y: -2 }}
      transition={{ duration: 0.2 }}
      className="p-5 sm:p-6 rounded-2xl bg-white dark:bg-[#121620] border border-neutral-200/80 dark:border-neutral-800/80 shadow-sm hover:shadow-md transition-all"
    >
      <div className="flex items-center justify-between">
        <span className="text-xs font-semibold uppercase tracking-wider text-neutral-500 dark:text-neutral-400">
          {label}
        </span>
        <div
          className={`w-9 h-9 rounded-xl flex items-center justify-center border ${config.iconBg}`}
        >
          <Icon className="w-4 h-4" />
        </div>
      </div>

      <div className="mt-4">
        {isLoading ? (
          <div className="h-8 w-32 bg-neutral-200 dark:bg-neutral-800 animate-pulse rounded-lg" />
        ) : (
          <div className={`text-2xl sm:text-3xl font-extrabold tracking-tight font-mono ${config.accentColor}`}>
            {type === 'outflow' && amount > 0 ? '-' : type === 'inflow' && amount > 0 ? '+' : ''}
            {formatCurrency(Math.abs(amount))}
          </div>
        )}

        <div className="mt-2 flex items-center justify-between text-xs text-neutral-500 dark:text-neutral-400">
          <span>{subtitle || (type === 'net' ? 'Net flow (Inflow - Outflow)' : 'Completed transactions')}</span>
        </div>
      </div>
    </motion.div>
  );
};
