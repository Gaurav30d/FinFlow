import React, { useState } from 'react';
import { motion } from 'framer-motion';
import {
  Eye,
  EyeOff,
  Copy,
  Check,
  ArrowUpRight,
  ArrowDownRight,
  CreditCard,
  RefreshCw
} from 'lucide-react';
import { Account } from '../types';
import { formatCurrency, maskAccountNumber } from '../utils/formatters';
import { useToast } from '../context/ToastContext';
import { DoodleSparkle, DoodleCoin } from './Doodles';

interface BalanceCardProps {
  account: Account | null;
  isLoading: boolean;
  onRefresh: () => void;
  onOpenTransfer: () => void;
  onOpenDeposit: () => void;
}

export const BalanceCard: React.FC<BalanceCardProps> = ({
  account,
  isLoading,
  onRefresh,
  onOpenTransfer,
  onOpenDeposit,
}) => {
  const { showToast } = useToast();
  const [showBalance, setShowBalance] = useState<boolean>(true);
  const [copied, setCopied] = useState<boolean>(false);

  const handleCopyAccount = () => {
    if (!account?.accountNumber) return;
    navigator.clipboard.writeText(account.accountNumber);
    setCopied(true);
    showToast('Account number copied to clipboard', 'info');
    setTimeout(() => setCopied(false), 2000);
  };

  const balance = account?.balance ?? 0;

  return (
    <div className="relative overflow-hidden rounded-3xl p-6 sm:p-8 bg-gradient-to-br from-[#0c121e] via-[#10192b] to-[#15233c] text-white shadow-xl border border-white/10 hover-lift">
      {/* Background glow effects with smooth floating animation */}
      <div className="absolute top-0 right-0 w-80 h-80 bg-blue-500/15 rounded-full blur-3xl pointer-events-none -mr-20 -mt-20 animate-float-drift" />
      <div className="absolute bottom-0 left-0 w-64 h-64 bg-indigo-500/15 rounded-full blur-2xl pointer-events-none -ml-20 -mb-20 animate-float-slow" />

      {/* Decorative hand-drawn doodles */}
      <div className="absolute top-4 right-28 pointer-events-none hidden sm:block animate-float-medium">
        <DoodleSparkle size={20} className="text-amber-400/50" />
      </div>
      <div className="absolute bottom-6 right-8 pointer-events-none hidden sm:block animate-float-fast opacity-40">
        <DoodleCoin size={26} className="text-blue-300/40" />
      </div>

      {/* Top row: Label, Account Number, Eye toggle */}
      <div className="relative z-10 flex flex-wrap items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-white/10 backdrop-blur-md flex items-center justify-center border border-white/15 text-blue-400">
            <CreditCard className="w-5 h-5" />
          </div>
          <div>
            <span className="text-xs font-semibold tracking-wider uppercase text-neutral-400">
              Primary Account
            </span>
            <div className="flex items-center gap-2 mt-0.5">
              <span className="text-sm font-mono text-neutral-200">
                {account ? maskAccountNumber(account.accountNumber) : '•••• ••••'}
              </span>
              {account?.accountNumber && (
                <button
                  onClick={handleCopyAccount}
                  title="Copy full account number"
                  className="text-neutral-400 hover:text-white transition-colors p-1"
                >
                  {copied ? (
                    <Check className="w-3.5 h-3.5 text-emerald-400" />
                  ) : (
                    <Copy className="w-3.5 h-3.5" />
                  )}
                </button>
              )}
            </div>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <button
            onClick={() => setShowBalance(!showBalance)}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-white/5 hover:bg-white/10 border border-white/10 text-xs font-medium text-neutral-300 hover:text-white transition-all backdrop-blur-sm"
          >
            {showBalance ? (
              <>
                <EyeOff className="w-3.5 h-3.5" />
                <span>Hide</span>
              </>
            ) : (
              <>
                <Eye className="w-3.5 h-3.5" />
                <span>Show</span>
              </>
            )}
          </button>

          <button
            onClick={onRefresh}
            disabled={isLoading}
            title="Refresh balance"
            className="p-2 rounded-lg bg-white/5 hover:bg-white/10 border border-white/10 text-neutral-300 hover:text-white transition-all disabled:opacity-50"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${isLoading ? 'animate-spin' : ''}`} />
          </button>
        </div>
      </div>

      {/* Main Balance Display */}
      <div className="relative z-10 my-6 sm:my-8">
        <span className="text-xs font-medium text-neutral-400 block mb-1">
          Available Balance
        </span>
        <div className="flex items-baseline gap-2">
          {isLoading && !account ? (
            <div className="h-10 w-48 bg-white/10 animate-pulse rounded-lg" />
          ) : (
            <motion.div
              key={showBalance ? 'shown' : 'hidden'}
              initial={{ opacity: 0, y: 5 }}
              animate={{ opacity: 1, y: 0 }}
              className="text-3xl sm:text-5xl font-extrabold tracking-tight text-white font-mono"
            >
              {showBalance ? formatCurrency(balance) : '₹••••••••'}
            </motion.div>
          )}
          <span className="text-xs font-bold px-2 py-0.5 rounded bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 uppercase tracking-wider">
            Active
          </span>
        </div>
        <p className="text-xs text-neutral-400 mt-2">
          Real-time balance for everyday spending and transfers
        </p>
      </div>

      {/* Quick Action Buttons */}
      <div className="relative z-10 flex flex-wrap items-center gap-3 pt-2 border-t border-white/10">
        <button
          onClick={onOpenTransfer}
          className="flex-1 sm:flex-initial flex items-center justify-center gap-2 px-5 py-2.5 rounded-xl bg-blue-600 hover:bg-blue-500 text-white font-medium text-sm transition-all shadow-lg shadow-blue-600/30 active:scale-[0.98]"
        >
          <ArrowUpRight className="w-4 h-4" />
          <span>Send Money</span>
        </button>

        <button
          onClick={onOpenDeposit}
          className="flex-1 sm:flex-initial flex items-center justify-center gap-2 px-5 py-2.5 rounded-xl bg-white/10 hover:bg-white/15 border border-white/15 text-white font-medium text-sm transition-all backdrop-blur-sm active:scale-[0.98]"
        >
          <ArrowDownRight className="w-4 h-4 text-emerald-400" />
          <span>Add Funds</span>
        </button>
      </div>
    </div>
  );
};
