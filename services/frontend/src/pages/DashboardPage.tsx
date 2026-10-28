import React, { useState, useEffect, useCallback } from 'react';
import { useOutletContext } from 'react-router-dom';
import {
  Bell,
  ArrowUpRight,
  ArrowDownRight,
  CheckCircle2,
  Calendar
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { transactionsApi, aiApi, notificationsApi } from '../api/client';
import type { Transaction, AiInsightsResponse, NotificationItem } from '../types';
import { BalanceCard } from '../components/BalanceCard';
import { FinancialMetricCard } from '../components/FinancialMetricCard';
import { CashFlowChart } from '../components/CashFlowChart';
import { RecentTransactionsList } from '../components/RecentTransactionsList';
import { AiInsightsCard } from '../components/AiInsightsCard';
import { formatRelativeDate } from '../utils/formatters';
import { DoodleSparkle, DoodleGrowth } from '../components/Doodles';

interface LayoutContextType {
  onOpenTransfer: () => void;
  onOpenDeposit: () => void;
}

export const DashboardPage: React.FC = () => {
  const { user, account, isLoading: isAuthLoading, refreshAccount } = useAuth();
  const { onOpenTransfer, onOpenDeposit } = useOutletContext<LayoutContextType>();

  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [isTransactionsLoading, setIsTransactionsLoading] = useState<boolean>(true);

  const [aiInsights, setAiInsights] = useState<AiInsightsResponse | null>(null);
  const [isAiLoading, setIsAiLoading] = useState<boolean>(false);
  const [aiError, setAiError] = useState<string | null>(null);

  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [isNotificationsLoading, setIsNotificationsLoading] = useState<boolean>(true);

  // Fetch transactions
  const loadTransactions = useCallback(async () => {
    setIsTransactionsLoading(true);
    try {
      const data = await transactionsApi.getMyTransactions();
      setTransactions(data);
    } catch (err) {
      console.error('Failed to load transactions', err);
    } finally {
      setIsTransactionsLoading(false);
    }
  }, []);

  // Fetch AI Insights
  const loadAiInsights = useCallback(async () => {
    setIsAiLoading(true);
    setAiError(null);
    try {
      const data = await aiApi.getInsights();
      setAiInsights(data);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to fetch AI insights';
      setAiError(msg);
    } finally {
      setIsAiLoading(false);
    }
  }, []);

  // Fetch notifications preview
  const loadNotifications = useCallback(async () => {
    setIsNotificationsLoading(true);
    try {
      const data = await notificationsApi.getNotifications();
      setNotifications(data.slice(0, 3));
    } catch (err) {
      console.error('Failed to load notifications', err);
    } finally {
      setIsNotificationsLoading(false);
    }
  }, []);

  useEffect(() => {
    loadTransactions();
    loadAiInsights();
    loadNotifications();
  }, [loadTransactions, loadAiInsights, loadNotifications]);

  // Compute financial metrics accurately from completed transactions
  const metrics = React.useMemo(() => {
    let inflow = 0;
    let outflow = 0;

    transactions.forEach((tx) => {
      if (tx.status === 'COMPLETED') {
        if (tx.type === 'DEPOSIT' || tx.type === 'TRANSFER_RECEIVED') {
          inflow += tx.amount;
        } else if (tx.type === 'WITHDRAW' || tx.type === 'TRANSFER_SENT') {
          outflow += tx.amount;
        }
      }
    });

    const net = inflow - outflow;
    return { inflow, outflow, net };
  }, [transactions]);

  const greeting = React.useMemo(() => {
    const hour = new Date().getHours();
    if (hour < 12) return 'Good morning';
    if (hour < 18) return 'Good afternoon';
    return 'Good evening';
  }, []);

  const todayStr = new Intl.DateTimeFormat('en-IN', {
    weekday: 'long',
    month: 'short',
    day: 'numeric',
    year: 'numeric',
  }).format(new Date());

  const handleRefreshAll = () => {
    refreshAccount();
    loadTransactions();
    loadAiInsights();
    loadNotifications();
  };

  return (
    <div className="space-y-6 sm:space-y-8 animate-fade-in">
      {/* Top Greeting and Date Banner */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 relative">
        <div className="relative">
          <div className="flex items-center gap-2.5">
            <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight text-neutral-900 dark:text-white">
              {greeting}, {user?.name || user?.username || 'Member'}
            </h1>
            <div className="hidden sm:inline-block animate-float-medium pointer-events-none">
              <DoodleSparkle size={22} className="text-amber-500/80 dark:text-amber-400/70" />
            </div>
          </div>
          <div className="flex items-center gap-2 text-xs sm:text-sm text-neutral-500 dark:text-neutral-400 mt-1">
            <Calendar className="w-4 h-4" />
            <span>{todayStr}</span>
            <span>•</span>
            <span className="flex items-center gap-1 text-emerald-600 dark:text-emerald-400 font-medium">
              <CheckCircle2 className="w-3.5 h-3.5" />
              Account Active & Verified
            </span>
          </div>
        </div>

        {/* Quick action buttons on header */}
        <div className="flex items-center gap-2">
          <button
            onClick={onOpenTransfer}
            className="flex items-center gap-1.5 px-3.5 py-2 bg-blue-600 hover:bg-blue-500 text-white text-xs font-semibold rounded-xl shadow-xs transition-all active:scale-95 hover-lift"
          >
            <ArrowUpRight className="w-4 h-4" />
            <span>Send Money</span>
          </button>
          <button
            onClick={onOpenDeposit}
            className="flex items-center gap-1.5 px-3.5 py-2 bg-neutral-100 hover:bg-neutral-200 dark:bg-neutral-800 dark:hover:bg-neutral-700 text-neutral-800 dark:text-neutral-200 text-xs font-semibold rounded-xl border border-neutral-200 dark:border-neutral-700 transition-all active:scale-95 hover-lift"
          >
            <ArrowDownRight className="w-4 h-4 text-emerald-500" />
            <span>Add Funds</span>
          </button>
        </div>
      </div>

      {/* Hero Section: Balance Card & Financial Metric Cards */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        {/* Left Column: Authoritative Balance Card */}
        <div className="lg:col-span-7">
          <BalanceCard
            account={account}
            isLoading={isAuthLoading}
            onRefresh={handleRefreshAll}
            onOpenTransfer={onOpenTransfer}
            onOpenDeposit={onOpenDeposit}
          />
        </div>

        {/* Right Column: Three Financial Metric Cards */}
        <div className="lg:col-span-5 grid grid-cols-1 sm:grid-cols-3 lg:grid-cols-1 gap-4">
          <FinancialMetricCard
            type="inflow"
            amount={metrics.inflow}
            label="Total Inflow"
            subtitle="Deposits & incoming transfers"
            isLoading={isTransactionsLoading}
          />
          <FinancialMetricCard
            type="outflow"
            amount={metrics.outflow}
            label="Total Outflow"
            subtitle="Sent transfers & withdrawals"
            isLoading={isTransactionsLoading}
          />
          <FinancialMetricCard
            type="net"
            amount={metrics.net}
            label="Net Cash Flow"
            subtitle="Inflow minus Outflow"
            isLoading={isTransactionsLoading}
          />
        </div>
      </div>

      {/* Prominent Gemini AI Financial Insights */}
      <div>
        <AiInsightsCard
          insights={aiInsights}
          isLoading={isAiLoading}
          error={aiError}
          onRefresh={loadAiInsights}
        />
      </div>

      {/* Interactive Charts and Recent Transactions */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        <div className="lg:col-span-7">
          <CashFlowChart
            transactions={transactions}
            currentBalance={account?.balance ?? 0}
          />
        </div>
        <div className="lg:col-span-5">
          <RecentTransactionsList
            transactions={transactions}
            isLoading={isTransactionsLoading}
            onRefresh={loadTransactions}
          />
        </div>
      </div>

      {/* Notification Activity Drawer Preview if unread exists */}
      {notifications.length > 0 && (
        <div className="p-5 rounded-2xl bg-white dark:bg-[#121620] border border-neutral-200/80 dark:border-neutral-800/80 shadow-sm">
          <div className="flex items-center justify-between mb-3">
            <div className="flex items-center gap-2">
              <Bell className="w-4 h-4 text-neutral-500 dark:text-neutral-400" />
              <h3 className="text-sm font-semibold text-neutral-900 dark:text-white">
                Recent Activity & Security Alerts
              </h3>
            </div>
            <span className="text-xs text-neutral-500 dark:text-neutral-400">
              {notifications.length} recent
            </span>
          </div>
          <div className="space-y-2">
            {notifications.map((n) => (
              <div
                key={n.id}
                className="p-3 rounded-xl bg-neutral-50 dark:bg-neutral-900/60 flex items-start justify-between gap-3 text-xs"
              >
                <div>
                  <p className="font-medium text-neutral-800 dark:text-neutral-200">
                    {n.title || n.message}
                  </p>
                  {n.title && (
                    <p className="text-neutral-500 dark:text-neutral-400 mt-0.5">
                      {n.message}
                    </p>
                  )}
                </div>
                <span className="text-neutral-400 shrink-0">
                  {formatRelativeDate(n.createdAt)}
                </span>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};
