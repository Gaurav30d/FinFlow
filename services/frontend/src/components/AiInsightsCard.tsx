import React from 'react';
import { Link } from 'react-router-dom';
import { Sparkles, RefreshCw, MessageSquare, Info, AlertCircle } from 'lucide-react';
import type { AiInsightsResponse } from '../types';

interface AiInsightsCardProps {
  insights: AiInsightsResponse | null;
  isLoading: boolean;
  error: string | null;
  onRefresh: () => void;
}

export const AiInsightsCard: React.FC<AiInsightsCardProps> = ({
  insights,
  isLoading,
  error,
  onRefresh,
}) => {
  return (
    <div className="relative overflow-hidden rounded-2xl p-6 bg-gradient-to-br from-indigo-900/10 via-purple-900/5 to-blue-900/10 dark:from-indigo-950/40 dark:via-purple-950/20 dark:to-blue-950/30 border border-indigo-200/50 dark:border-indigo-800/40 shadow-sm">
      {/* Decorative subtle ambient light */}
      <div className="absolute top-0 right-0 w-64 h-64 bg-indigo-500/10 rounded-full blur-3xl pointer-events-none -mr-20 -mt-20" />

      {/* Header */}
      <div className="relative z-10 flex items-center justify-between gap-4 mb-4">
        <div className="flex items-center gap-2.5">
          <div className="w-8 h-8 rounded-lg bg-indigo-500/15 dark:bg-indigo-400/20 text-indigo-600 dark:text-indigo-400 flex items-center justify-center border border-indigo-500/20">
            <Sparkles className="w-4 h-4" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h3 className="font-semibold text-neutral-900 dark:text-white text-sm sm:text-base">
                Financial Assistant Insights
              </h3>
              <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-indigo-100 dark:bg-indigo-950/80 text-indigo-700 dark:text-indigo-300 border border-indigo-200 dark:border-indigo-800/60 uppercase tracking-wider">
                Personalized
              </span>
            </div>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <button
            onClick={onRefresh}
            disabled={isLoading}
            title="Refresh Insights"
            className="p-1.5 rounded-lg bg-white/60 dark:bg-neutral-800/60 hover:bg-white dark:hover:bg-neutral-800 border border-neutral-200/60 dark:border-neutral-700/60 text-neutral-600 dark:text-neutral-300 transition-all disabled:opacity-50"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${isLoading ? 'animate-spin' : ''}`} />
          </button>
          <Link
            to="/dashboard/ai-advisor"
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-indigo-600 hover:bg-indigo-700 text-white text-xs font-medium transition-all shadow-xs"
          >
            <MessageSquare className="w-3.5 h-3.5" />
            <span className="hidden sm:inline">Ask Assistant</span>
          </Link>
        </div>
      </div>

      {/* Content */}
      <div className="relative z-10">
        {isLoading ? (
          <div className="space-y-2.5 py-2">
            <div className="h-4 w-3/4 bg-indigo-200/40 dark:bg-indigo-900/40 rounded animate-pulse" />
            <div className="h-4 w-5/6 bg-indigo-200/40 dark:bg-indigo-900/40 rounded animate-pulse" />
            <div className="h-4 w-1/2 bg-indigo-200/40 dark:bg-indigo-900/40 rounded animate-pulse" />
          </div>
        ) : error ? (
          <div className="p-3.5 rounded-xl bg-amber-50 dark:bg-amber-950/40 border border-amber-200 dark:border-amber-800/40 text-amber-800 dark:text-amber-300 text-xs flex items-start gap-2.5">
            <AlertCircle className="w-4 h-4 shrink-0 mt-0.5 text-amber-600 dark:text-amber-400" />
            <div>
              <p className="font-medium">Unable to load spending insights right now.</p>
              <p className="mt-0.5 text-amber-700 dark:text-amber-400">{error}</p>
              <button
                onClick={onRefresh}
                className="mt-2 text-indigo-600 dark:text-indigo-400 font-semibold underline underline-offset-2"
              >
                Retry analysis
              </button>
            </div>
          </div>
        ) : insights ? (
          <div className="space-y-3">
            <div className="text-sm leading-relaxed text-neutral-700 dark:text-neutral-200 whitespace-pre-line font-normal">
              {insights.insights}
            </div>

            {/* Quick summary indicators */}
            {insights.summary && (
              <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 pt-3 border-t border-indigo-200/40 dark:border-indigo-800/30">
                <div className="bg-white/60 dark:bg-neutral-900/60 p-2 rounded-lg border border-neutral-200/50 dark:border-neutral-800/50">
                  <span className="text-[10px] text-neutral-500 dark:text-neutral-400 block uppercase">
                    Transactions
                  </span>
                  <span className="text-xs font-bold text-neutral-800 dark:text-neutral-200">
                    {insights.summary.completedTransactionCount} Recorded
                  </span>
                </div>
                <div className="bg-white/60 dark:bg-neutral-900/60 p-2 rounded-lg border border-neutral-200/50 dark:border-neutral-800/50">
                  <span className="text-[10px] text-neutral-500 dark:text-neutral-400 block uppercase">
                    Money In
                  </span>
                  <span className="text-xs font-bold text-emerald-600 dark:text-emerald-400 font-mono">
                    ₹{insights.summary.totalInflow.toFixed(2)}
                  </span>
                </div>
                <div className="bg-white/60 dark:bg-neutral-900/60 p-2 rounded-lg border border-neutral-200/50 dark:border-neutral-800/50">
                  <span className="text-[10px] text-neutral-500 dark:text-neutral-400 block uppercase">
                    Money Out
                  </span>
                  <span className="text-xs font-bold text-rose-600 dark:text-rose-400 font-mono">
                    ₹{insights.summary.totalOutflow.toFixed(2)}
                  </span>
                </div>
                <div className="bg-white/60 dark:bg-neutral-900/60 p-2 rounded-lg border border-neutral-200/50 dark:border-neutral-800/50">
                  <span className="text-[10px] text-neutral-500 dark:text-neutral-400 block uppercase">
                    Net Cash Flow
                  </span>
                  <span
                    className={`text-xs font-bold font-mono ${
                      insights.summary.netCashFlow >= 0
                        ? 'text-blue-600 dark:text-blue-400'
                        : 'text-amber-600 dark:text-amber-400'
                    }`}
                  >
                    ₹{insights.summary.netCashFlow.toFixed(2)}
                  </span>
                </div>
              </div>
            )}
          </div>
        ) : (
          <p className="text-xs text-neutral-500 dark:text-neutral-400">
            Click refresh to generate smart insights based on your transaction history.
          </p>
        )}

        {/* Disclaimer */}
        <div className="mt-3.5 flex items-center gap-1.5 text-[11px] text-neutral-400 dark:text-neutral-500">
          <Info className="w-3.5 h-3.5 shrink-0" />
          <span>FinFlow Financial Assistant summaries are informational and designed to help guide your everyday habits.</span>
        </div>
      </div>
    </div>
  );
};
