import React from 'react';
import { Link } from 'react-router-dom';
import {
  ArrowDownLeft,
  ArrowUpRight,
  ArrowDownRight,
  Wallet,
  ArrowRight,
  Receipt
} from 'lucide-react';
import { Transaction } from '../types';
import { formatCurrency, formatRelativeDate, getTransactionTypeLabel } from '../utils/formatters';

interface RecentTransactionsListProps {
  transactions: Transaction[];
  isLoading: boolean;
  onRefresh?: () => void;
}

export const RecentTransactionsList: React.FC<RecentTransactionsListProps> = ({
  transactions,
  isLoading,
}) => {
  const getIcon = (type: Transaction['type']) => {
    switch (type) {
      case 'DEPOSIT':
        return <ArrowDownRight className="w-4 h-4 text-emerald-600 dark:text-emerald-400" />;
      case 'WITHDRAW':
        return <ArrowUpRight className="w-4 h-4 text-rose-600 dark:text-rose-400" />;
      case 'TRANSFER_SENT':
        return <ArrowUpRight className="w-4 h-4 text-rose-600 dark:text-rose-400" />;
      case 'TRANSFER_RECEIVED':
        return <ArrowDownLeft className="w-4 h-4 text-emerald-600 dark:text-emerald-400" />;
      default:
        return <Wallet className="w-4 h-4 text-blue-600 dark:text-blue-400" />;
    }
  };

  const getIconBg = (type: Transaction['type']) => {
    switch (type) {
      case 'DEPOSIT':
      case 'TRANSFER_RECEIVED':
        return 'bg-emerald-50 dark:bg-emerald-950/40 border-emerald-100 dark:border-emerald-800/40';
      case 'WITHDRAW':
      case 'TRANSFER_SENT':
        return 'bg-rose-50 dark:bg-rose-950/40 border-rose-100 dark:border-rose-800/40';
      default:
        return 'bg-blue-50 dark:bg-blue-950/40 border-blue-100 dark:border-blue-800/40';
    }
  };

  const isPositive = (type: Transaction['type']) => {
    return type === 'DEPOSIT' || type === 'TRANSFER_RECEIVED';
  };

  return (
    <div className="p-6 rounded-2xl bg-white dark:bg-[#121620] border border-neutral-200/80 dark:border-neutral-800/80 shadow-sm">
      <div className="flex items-center justify-between mb-4">
        <div>
          <h3 className="font-semibold text-neutral-900 dark:text-white">
            Recent Transactions
          </h3>
          <p className="text-xs text-neutral-500 dark:text-neutral-400">
            Latest account activity and transfers
          </p>
        </div>
        <Link
          to="/dashboard/transactions"
          className="text-xs font-semibold text-blue-600 dark:text-blue-400 hover:text-blue-700 flex items-center gap-1 transition-colors"
        >
          <span>View all</span>
          <ArrowRight className="w-3.5 h-3.5" />
        </Link>
      </div>

      {isLoading ? (
        <div className="space-y-3 py-2">
          {[1, 2, 3, 4, 5].map((i) => (
            <div
              key={i}
              className="flex items-center justify-between p-3 rounded-xl bg-neutral-50/60 dark:bg-neutral-900/40 animate-pulse"
            >
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-xl bg-neutral-200 dark:bg-neutral-800" />
                <div className="space-y-1.5">
                  <div className="w-28 h-3.5 bg-neutral-200 dark:bg-neutral-800 rounded" />
                  <div className="w-20 h-2.5 bg-neutral-200 dark:bg-neutral-800 rounded" />
                </div>
              </div>
              <div className="w-20 h-4 bg-neutral-200 dark:bg-neutral-800 rounded" />
            </div>
          ))}
        </div>
      ) : transactions.length === 0 ? (
        <div className="text-center py-10 px-4 rounded-xl border border-dashed border-neutral-200 dark:border-neutral-800 bg-neutral-50/50 dark:bg-neutral-900/30">
          <div className="w-12 h-12 rounded-xl bg-neutral-100 dark:bg-neutral-800 text-neutral-400 mx-auto flex items-center justify-center mb-3">
            <Receipt className="w-6 h-6" />
          </div>
          <p className="text-sm font-semibold text-neutral-800 dark:text-neutral-200">
            No transactions yet
          </p>
          <p className="text-xs text-neutral-500 dark:text-neutral-400 mt-1 max-w-xs mx-auto">
            Your recent deposits and transfers will automatically appear here once initiated.
          </p>
        </div>
      ) : (
        <div className="divide-y divide-neutral-100 dark:divide-neutral-800/60">
          {transactions.slice(0, 6).map((tx) => {
            const positive = isPositive(tx.type);
            return (
              <div
                key={tx.id || tx.transactionReference}
                className="py-3 sm:py-3.5 flex items-center justify-between gap-3 hover:bg-neutral-50/60 dark:hover:bg-neutral-800/30 px-2 rounded-xl transition-colors"
              >
                <div className="flex items-center gap-3 min-w-0">
                  <div
                    className={`w-10 h-10 rounded-xl flex items-center justify-center border shrink-0 ${getIconBg(
                      tx.type
                    )}`}
                  >
                    {getIcon(tx.type)}
                  </div>
                  <div className="min-w-0">
                    <p className="text-sm font-medium text-neutral-900 dark:text-white truncate">
                      {tx.description || getTransactionTypeLabel(tx.type)}
                    </p>
                    <div className="flex items-center gap-2 text-xs text-neutral-500 dark:text-neutral-400 mt-0.5">
                      <span>{formatRelativeDate(tx.createdAt)}</span>
                      <span>•</span>
                      <span className="font-mono text-[11px] truncate max-w-[120px]">
                        Ref: {tx.transactionReference?.substring(0, 8)}
                      </span>
                    </div>
                  </div>
                </div>

                <div className="text-right shrink-0">
                  <div
                    className={`text-sm font-bold font-mono ${
                      positive
                        ? 'text-emerald-600 dark:text-emerald-400'
                        : 'text-neutral-900 dark:text-white'
                    }`}
                  >
                    {positive ? '+' : '-'}
                    {formatCurrency(tx.amount)}
                  </div>
                  <div className="mt-0.5">
                    <span
                      className={`inline-block text-[10px] font-semibold uppercase px-1.5 py-0.2 rounded ${
                        tx.status === 'COMPLETED'
                          ? 'bg-emerald-50 dark:bg-emerald-950/40 text-emerald-600 dark:text-emerald-400 border border-emerald-200/50 dark:border-emerald-800/40'
                          : tx.status === 'PENDING'
                          ? 'bg-amber-50 dark:bg-amber-950/40 text-amber-600 dark:text-amber-400 border border-amber-200/50 dark:border-amber-800/40'
                          : 'bg-rose-50 dark:bg-rose-950/40 text-rose-600 dark:text-rose-400 border border-rose-200/50 dark:border-rose-800/40'
                      }`}
                    >
                      {tx.status}
                    </span>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};
