import React, { useState, useMemo } from 'react';
import {
  ResponsiveContainer,
  AreaChart,
  Area,
  XAxis,
  YAxis,
  Tooltip,
  CartesianGrid,
} from 'recharts';
import { Calendar, TrendingUp } from 'lucide-react';
import { Transaction } from '../types';
import { useTheme } from '../context/ThemeContext';
import { formatCurrency } from '../utils/formatters';

interface CashFlowChartProps {
  transactions: Transaction[];
  currentBalance: number;
}

export const CashFlowChart: React.FC<CashFlowChartProps> = ({
  transactions,
}) => {
  const { theme } = useTheme();
  const [filter, setFilter] = useState<'7D' | '30D' | 'ALL'>('30D');

  // Compute daily inflow and outflow points from real transactions
  const chartData = useMemo(() => {
    if (!transactions || transactions.length === 0) return [];

    const now = new Date();
    const days = filter === '7D' ? 7 : filter === '30D' ? 30 : 90;
    const cutoff = new Date(now.getTime() - days * 24 * 60 * 60 * 1000);

    // Group completed transactions by day (YYYY-MM-DD)
    const map = new Map<string, { date: string; displayDate: string; inflow: number; outflow: number }>();

    // Generate consecutive dates within range to give continuous flow
    for (let i = days; i >= 0; i--) {
      const d = new Date(now.getTime() - i * 24 * 60 * 60 * 1000);
      const isoKey = d.toISOString().split('T')[0];
      const displayDate = d.toLocaleDateString('en-IN', {
        month: 'short',
        day: 'numeric',
      });
      map.set(isoKey, { date: isoKey, displayDate, inflow: 0, outflow: 0 });
    }

    transactions.forEach((tx) => {
      if (tx.status !== 'COMPLETED') return;
      const txDate = new Date(tx.createdAt);
      if (txDate < cutoff) return;

      const isoKey = tx.createdAt.split('T')[0];
      const existing = map.get(isoKey);
      if (existing) {
        if (tx.type === 'DEPOSIT' || tx.type === 'TRANSFER_RECEIVED') {
          existing.inflow += tx.amount;
        } else if (tx.type === 'WITHDRAW' || tx.type === 'TRANSFER_SENT') {
          existing.outflow += tx.amount;
        }
      }
    });

    return Array.from(map.values()).sort((a, b) => a.date.localeCompare(b.date));
  }, [transactions, filter]);

  const hasActivity = chartData.some((d) => d.inflow > 0 || d.outflow > 0);

  const isDark = theme === 'dark';
  const gridColor = isDark ? '#1e2638' : '#e5e7eb';
  const textColor = isDark ? '#94a3b8' : '#64748b';

  return (
    <div className="p-6 rounded-2xl bg-white dark:bg-[#121620] border border-neutral-200/80 dark:border-neutral-800/80 shadow-sm flex flex-col justify-between">
      {/* Chart Header */}
      <div className="flex flex-wrap items-center justify-between gap-4 mb-6">
        <div>
          <div className="flex items-center gap-2">
            <h3 className="font-semibold text-neutral-900 dark:text-white">
              Cash Flow Activity
            </h3>
            <span className="text-xs px-2 py-0.5 rounded-full bg-blue-50 dark:bg-blue-950/40 text-blue-600 dark:text-blue-400 border border-blue-200 dark:border-blue-800/40 font-medium">
              Real-time
            </span>
          </div>
          <p className="text-xs text-neutral-500 dark:text-neutral-400 mt-0.5">
            Daily inflows and outflows from recorded transactions
          </p>
        </div>

        {/* Filter buttons */}
        <div className="flex items-center gap-1 bg-neutral-100 dark:bg-neutral-900 p-1 rounded-xl border border-neutral-200 dark:border-neutral-800 text-xs">
          {(['7D', '30D', 'ALL'] as const).map((period) => (
            <button
              key={period}
              onClick={() => setFilter(period)}
              className={`px-3 py-1 font-medium rounded-lg transition-all ${
                filter === period
                  ? 'bg-white dark:bg-neutral-800 text-neutral-900 dark:text-white shadow-xs'
                  : 'text-neutral-500 hover:text-neutral-900 dark:hover:text-white'
              }`}
            >
              {period}
            </button>
          ))}
        </div>
      </div>

      {/* Chart Container */}
      {!hasActivity ? (
        <div className="h-64 flex flex-col items-center justify-center text-center p-6 rounded-xl border border-dashed border-neutral-200 dark:border-neutral-800 bg-neutral-50/50 dark:bg-neutral-900/30">
          <div className="w-12 h-12 rounded-xl bg-neutral-100 dark:bg-neutral-800 flex items-center justify-center text-neutral-400 mb-3">
            <TrendingUp className="w-6 h-6" />
          </div>
          <p className="text-sm font-medium text-neutral-800 dark:text-neutral-200">
            No Cash Flow in this timeframe
          </p>
          <p className="text-xs text-neutral-500 dark:text-neutral-400 mt-1 max-w-xs">
            Complete deposits or transfers to see your daily financial momentum visualized.
          </p>
        </div>
      ) : (
        <div className="h-64 w-full">
          <ResponsiveContainer width="100%" height="100%">
            <AreaChart data={chartData} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
              <defs>
                <linearGradient id="inflowGrad" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="#10b981" stopOpacity={0.3} />
                  <stop offset="95%" stopColor="#10b981" stopOpacity={0.0} />
                </linearGradient>
                <linearGradient id="outflowGrad" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="#f43f5e" stopOpacity={0.3} />
                  <stop offset="95%" stopColor="#f43f5e" stopOpacity={0.0} />
                </linearGradient>
              </defs>

              <CartesianGrid strokeDasharray="3 3" vertical={false} stroke={gridColor} />

              <XAxis
                dataKey="displayDate"
                stroke={textColor}
                fontSize={11}
                tickLine={false}
                axisLine={false}
                interval="preserveStartEnd"
              />

              <YAxis
                stroke={textColor}
                fontSize={11}
                tickLine={false}
                axisLine={false}
                tickFormatter={(val) => (val >= 1000 ? `₹${val / 1000}k` : `₹${val}`)}
              />

              <Tooltip
                content={({ active, payload, label }) => {
                  if (active && payload && payload.length) {
                    return (
                      <div className="bg-white dark:bg-[#161c28] border border-neutral-200 dark:border-neutral-700 p-3 rounded-xl shadow-xl text-xs space-y-1.5">
                        <div className="font-semibold text-neutral-800 dark:text-neutral-200 flex items-center gap-1.5 border-b border-neutral-100 dark:border-neutral-800 pb-1">
                          <Calendar className="w-3.5 h-3.5 text-neutral-400" />
                          <span>{label}</span>
                        </div>
                        <div className="flex items-center justify-between gap-4 text-emerald-600 dark:text-emerald-400">
                          <span>Inflow:</span>
                          <span className="font-mono font-bold">
                            {formatCurrency(Number(payload[0]?.value || 0))}
                          </span>
                        </div>
                        <div className="flex items-center justify-between gap-4 text-rose-600 dark:text-rose-400">
                          <span>Outflow:</span>
                          <span className="font-mono font-bold">
                            {formatCurrency(Number(payload[1]?.value || 0))}
                          </span>
                        </div>
                      </div>
                    );
                  }
                  return null;
                }}
              />

              <Area
                type="monotone"
                dataKey="inflow"
                name="Inflow"
                stroke="#10b981"
                strokeWidth={2}
                fillOpacity={1}
                fill="url(#inflowGrad)"
              />

              <Area
                type="monotone"
                dataKey="outflow"
                name="Outflow"
                stroke="#f43f5e"
                strokeWidth={2}
                fillOpacity={1}
                fill="url(#outflowGrad)"
              />
            </AreaChart>
          </ResponsiveContainer>
        </div>
      )}

      {/* Legend */}
      <div className="flex items-center justify-center gap-6 mt-4 pt-3 border-t border-neutral-100 dark:border-neutral-800 text-xs">
        <div className="flex items-center gap-2">
          <span className="w-2.5 h-2.5 rounded-full bg-emerald-500" />
          <span className="text-neutral-600 dark:text-neutral-300">Inflows (Deposits & Received)</span>
        </div>
        <div className="flex items-center gap-2">
          <span className="w-2.5 h-2.5 rounded-full bg-rose-500" />
          <span className="text-neutral-600 dark:text-neutral-300">Outflows (Transfers & Withdrawals)</span>
        </div>
      </div>
    </div>
  );
};
