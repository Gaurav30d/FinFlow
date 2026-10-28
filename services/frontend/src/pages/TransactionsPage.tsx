import React, { useState, useEffect, useMemo, useCallback } from 'react';
import {
  Search,
  ArrowDownLeft,
  ArrowUpRight,
  ArrowDownRight,
  Wallet,
  RefreshCw,
  Receipt,
  Download,
  AlertCircle
} from 'lucide-react';
import { transactionsApi } from '../api/client';
import type { Transaction } from '../types';
import { formatCurrency, formatFullDateTime, getTransactionTypeLabel } from '../utils/formatters';

export const TransactionsPage: React.FC = () => {
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  // Filters
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [typeFilter, setTypeFilter] = useState<string>('ALL');
  const [statusFilter, setStatusFilter] = useState<string>('ALL');

  const loadTransactions = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await transactionsApi.getMyTransactions();
      setTransactions(data);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to fetch transactions';
      setError(msg);
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    loadTransactions();
  }, [loadTransactions]);

  const filteredTransactions = useMemo(() => {
    return transactions.filter((tx) => {
      // Type match
      if (typeFilter !== 'ALL' && tx.type !== typeFilter) return false;
      // Status match
      if (statusFilter !== 'ALL' && tx.status !== statusFilter) return false;
      // Search query match
      if (searchQuery.trim()) {
        const q = searchQuery.toLowerCase();
        const matchesRef = tx.transactionReference?.toLowerCase().includes(q);
        const matchesDesc = tx.description?.toLowerCase().includes(q);
        const matchesType = tx.type?.toLowerCase().includes(q);
        const matchesAmount = tx.amount?.toString().includes(q);
        if (!matchesRef && !matchesDesc && !matchesType && !matchesAmount) {
          return false;
        }
      }
      return true;
    });
  }, [transactions, typeFilter, statusFilter, searchQuery]);

  const getIcon = (type: Transaction['type']) => {
    switch (type) {
      case 'DEPOSIT':
        return <ArrowDownRight className="w-4 h-4 text-emerald-600 dark:text-emerald-400" />;
      case 'WITHDRAW':
      case 'TRANSFER_SENT':
        return <ArrowUpRight className="w-4 h-4 text-rose-600 dark:text-rose-400" />;
      case 'TRANSFER_RECEIVED':
        return <ArrowDownLeft className="w-4 h-4 text-emerald-600 dark:text-emerald-400" />;
      default:
        return <Wallet className="w-4 h-4 text-blue-600 dark:text-blue-400" />;
    }
  };

  const isPositive = (type: Transaction['type']) => {
    return type === 'DEPOSIT' || type === 'TRANSFER_RECEIVED';
  };

  const handleExportCSV = () => {
    if (filteredTransactions.length === 0) return;
    const headers = ['Reference,Type,Amount,Currency,Status,Description,Date\n'];
    const rows = filteredTransactions.map((tx) =>
      `"${tx.transactionReference}","${tx.type}","${tx.amount}","${tx.currency}","${tx.status}","${tx.description || ''}","${tx.createdAt}"`
    );
    const blob = new Blob([headers.join('') + rows.join('\n')], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `finflow-transactions-${new Date().toISOString().split('T')[0]}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  };

  return (
    <div className="space-y-6 animate-fade-in">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight text-neutral-900 dark:text-white">
            Transactions
          </h1>
          <p className="text-xs sm:text-sm text-neutral-500 dark:text-neutral-400 mt-1">
            Review your full history of deposits, withdrawals, and transfers
          </p>
        </div>

        <div className="flex items-center gap-2">
          <button
            onClick={handleExportCSV}
            disabled={filteredTransactions.length === 0}
            className="flex items-center gap-1.5 px-3 py-2 rounded-xl bg-white dark:bg-neutral-800 border border-neutral-200 dark:border-neutral-700 text-xs font-semibold text-neutral-700 dark:text-neutral-200 hover:bg-neutral-50 dark:hover:bg-neutral-700 transition-all disabled:opacity-50"
          >
            <Download className="w-3.5 h-3.5" />
            <span className="hidden sm:inline">Export CSV</span>
          </button>

          <button
            onClick={loadTransactions}
            disabled={isLoading}
            className="p-2 rounded-xl bg-white dark:bg-neutral-800 border border-neutral-200 dark:border-neutral-700 text-neutral-700 dark:text-neutral-200 hover:bg-neutral-50 dark:hover:bg-neutral-700 transition-all disabled:opacity-50"
          >
            <RefreshCw className={`w-4 h-4 ${isLoading ? 'animate-spin' : ''}`} />
          </button>
        </div>
      </div>

      {/* Filter and Search Bar */}
      <div className="p-4 rounded-2xl bg-white dark:bg-[#121620] border border-neutral-200/80 dark:border-neutral-800/80 shadow-sm space-y-3 sm:space-y-0 sm:flex sm:items-center sm:gap-4">
        {/* Search */}
        <div className="relative flex-1">
          <Search className="w-4 h-4 text-neutral-400 absolute left-3 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Search by reference, description, amount..."
            className="w-full pl-9 pr-4 py-2 text-xs sm:text-sm bg-neutral-50 dark:bg-neutral-900/60 border border-neutral-200 dark:border-neutral-800 rounded-xl text-neutral-900 dark:text-white placeholder-neutral-400 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500"
          />
        </div>

        {/* Filters */}
        <div className="flex items-center gap-2">
          <select
            value={typeFilter}
            onChange={(e) => setTypeFilter(e.target.value)}
            className="px-3 py-2 text-xs bg-neutral-50 dark:bg-neutral-900/60 border border-neutral-200 dark:border-neutral-800 rounded-xl text-neutral-800 dark:text-neutral-200 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
          >
            <option value="ALL">All Types</option>
            <option value="DEPOSIT">Deposits</option>
            <option value="WITHDRAW">Withdrawals</option>
            <option value="TRANSFER_SENT">Transfers Sent</option>
            <option value="TRANSFER_RECEIVED">Transfers Received</option>
          </select>

          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="px-3 py-2 text-xs bg-neutral-50 dark:bg-neutral-900/60 border border-neutral-200 dark:border-neutral-800 rounded-xl text-neutral-800 dark:text-neutral-200 focus:outline-none focus:ring-2 focus:ring-blue-500/20"
          >
            <option value="ALL">All Statuses</option>
            <option value="COMPLETED">Completed</option>
            <option value="PENDING">Pending</option>
            <option value="FAILED">Failed</option>
          </select>
        </div>
      </div>

      {/* Transactions Container */}
      <div className="rounded-2xl bg-white dark:bg-[#121620] border border-neutral-200/80 dark:border-neutral-800/80 shadow-sm overflow-hidden">
        {isLoading ? (
          <div className="p-8 space-y-4">
            {[1, 2, 3, 4, 5, 6].map((i) => (
              <div
                key={i}
                className="flex items-center justify-between p-3 rounded-xl bg-neutral-50/60 dark:bg-neutral-900/40 animate-pulse"
              >
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 rounded-xl bg-neutral-200 dark:bg-neutral-800" />
                  <div className="space-y-2">
                    <div className="w-32 h-4 bg-neutral-200 dark:bg-neutral-800 rounded" />
                    <div className="w-24 h-3 bg-neutral-200 dark:bg-neutral-800 rounded" />
                  </div>
                </div>
                <div className="w-24 h-5 bg-neutral-200 dark:bg-neutral-800 rounded" />
              </div>
            ))}
          </div>
        ) : error ? (
          <div className="p-12 text-center">
            <div className="w-12 h-12 rounded-xl bg-rose-50 dark:bg-rose-950/40 text-rose-600 dark:text-rose-400 mx-auto flex items-center justify-center mb-3">
              <AlertCircle className="w-6 h-6" />
            </div>
            <h3 className="font-semibold text-neutral-900 dark:text-white">
              Failed to load transactions
            </h3>
            <p className="text-xs text-neutral-500 dark:text-neutral-400 mt-1 max-w-sm mx-auto">
              {error}
            </p>
            <button
              onClick={loadTransactions}
              className="mt-4 px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white text-xs font-semibold rounded-xl"
            >
              Try Again
            </button>
          </div>
        ) : filteredTransactions.length === 0 ? (
          <div className="p-12 text-center">
            <div className="w-12 h-12 rounded-xl bg-neutral-100 dark:bg-neutral-800 text-neutral-400 mx-auto flex items-center justify-center mb-3">
              <Receipt className="w-6 h-6" />
            </div>
            <h3 className="font-semibold text-neutral-900 dark:text-white">
              No transactions found
            </h3>
            <p className="text-xs text-neutral-500 dark:text-neutral-400 mt-1 max-w-sm mx-auto">
              {searchQuery || typeFilter !== 'ALL' || statusFilter !== 'ALL'
                ? 'Try adjusting your search query or filters to find what you are looking for.'
                : 'You have not made any deposits or transfers yet.'}
            </p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-neutral-100 dark:border-neutral-800/80 bg-neutral-50/50 dark:bg-neutral-900/40 text-[11px] uppercase tracking-wider text-neutral-500 dark:text-neutral-400 font-semibold">
                  <th className="py-3 px-4 sm:px-6">Transaction</th>
                  <th className="py-3 px-4 hidden md:table-cell">Reference</th>
                  <th className="py-3 px-4 hidden sm:table-cell">Date & Time</th>
                  <th className="py-3 px-4">Status</th>
                  <th className="py-3 px-4 sm:px-6 text-right">Amount</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-neutral-100 dark:divide-neutral-800/60 text-xs sm:text-sm">
                {filteredTransactions.map((tx) => {
                  const positive = isPositive(tx.type);
                  return (
                    <tr
                      key={tx.id || tx.transactionReference}
                      className="hover:bg-neutral-50/70 dark:hover:bg-neutral-800/40 transition-colors"
                    >
                      <td className="py-4 px-4 sm:px-6">
                        <div className="flex items-center gap-3">
                          <div
                            className={`w-9 h-9 rounded-xl flex items-center justify-center border shrink-0 ${
                              positive
                                ? 'bg-emerald-50 dark:bg-emerald-950/40 border-emerald-100 dark:border-emerald-800/40'
                                : 'bg-rose-50 dark:bg-rose-950/40 border-rose-100 dark:border-rose-800/40'
                            }`}
                          >
                            {getIcon(tx.type)}
                          </div>
                          <div>
                            <p className="font-semibold text-neutral-900 dark:text-white">
                              {tx.description || getTransactionTypeLabel(tx.type)}
                            </p>
                            <span className="text-xs text-neutral-400 md:hidden block mt-0.5 font-mono">
                              {tx.transactionReference?.substring(0, 10)}...
                            </span>
                          </div>
                        </div>
                      </td>

                      <td className="py-4 px-4 font-mono text-xs text-neutral-500 dark:text-neutral-400 hidden md:table-cell">
                        {tx.transactionReference}
                      </td>

                      <td className="py-4 px-4 text-xs text-neutral-500 dark:text-neutral-400 hidden sm:table-cell">
                        {formatFullDateTime(tx.createdAt)}
                      </td>

                      <td className="py-4 px-4">
                        <span
                          className={`inline-block text-[11px] font-semibold uppercase px-2 py-0.5 rounded-full ${
                            tx.status === 'COMPLETED'
                              ? 'bg-emerald-50 dark:bg-emerald-950/40 text-emerald-600 dark:text-emerald-400 border border-emerald-200 dark:border-emerald-800/40'
                              : tx.status === 'PENDING'
                              ? 'bg-amber-50 dark:bg-amber-950/40 text-amber-600 dark:text-amber-400 border border-amber-200 dark:border-amber-800/40'
                              : 'bg-rose-50 dark:bg-rose-950/40 text-rose-600 dark:text-rose-400 border border-rose-200 dark:border-rose-800/40'
                          }`}
                        >
                          {tx.status}
                        </span>
                      </td>

                      <td className="py-4 px-4 sm:px-6 text-right font-mono font-bold">
                        <span
                          className={
                            positive
                              ? 'text-emerald-600 dark:text-emerald-400'
                              : 'text-neutral-900 dark:text-white'
                          }
                        >
                          {positive ? '+' : '-'}
                          {formatCurrency(tx.amount)}
                        </span>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};
