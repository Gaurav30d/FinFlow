import React, { useState } from 'react';
import {
  Send,
  ArrowRight,
  CheckCircle2,
  AlertCircle,
  Loader2,
  ShieldCheck,
  Info
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { transactionsApi, generateIdempotencyKey } from '../api/client';
import { formatCurrency, maskAccountNumber } from '../utils/formatters';
import { useToast } from '../context/ToastContext';

export const TransfersPage: React.FC = () => {
  const { account, refreshAccount } = useAuth();
  const { showToast } = useToast();

  const [toAccountNumber, setToAccountNumber] = useState('');
  const [amount, setAmount] = useState('');
  const [description, setDescription] = useState('');

  const [step, setStep] = useState<'input' | 'review' | 'success'>('input');
  const [currentIdempotencyKey, setCurrentIdempotencyKey] = useState<string>('');
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  const handleStartReview = (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    const num = parseFloat(amount);
    if (isNaN(num) || num <= 0) {
      setError('Please enter a valid transfer amount');
      return;
    }
    if (account && num > account.balance) {
      setError(`Insufficient balance. Your available balance is ${formatCurrency(account.balance)}`);
      return;
    }
    if (!toAccountNumber.trim()) {
      setError('Recipient account number is required');
      return;
    }
    if (account && toAccountNumber.trim() === account.accountNumber) {
      setError('Cannot transfer funds to your own account');
      return;
    }

    if (!currentIdempotencyKey) {
      setCurrentIdempotencyKey(generateIdempotencyKey());
    }
    setStep('review');
  };

  const handleExecuteTransfer = async () => {
    setIsLoading(true);
    setError(null);

    try {
      await transactionsApi.transfer(
        {
          toAccountNumber: toAccountNumber.trim(),
          amount: parseFloat(amount),
          description: description.trim() || undefined,
        },
        undefined,
        'INR',
        currentIdempotencyKey
      );

      setStep('success');
      showToast('Transfer completed successfully', 'success');
      refreshAccount();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Transfer failed. Check network or account status.';
      setError(msg);
      showToast(msg, 'error');
    } finally {
      setIsLoading(false);
    }
  };

  const handleReset = () => {
    setToAccountNumber('');
    setAmount('');
    setDescription('');
    setCurrentIdempotencyKey('');
    setError(null);
    setStep('input');
  };

  return (
    <div className="space-y-6 sm:space-y-8 animate-fade-in max-w-4xl mx-auto">
      {/* Header */}
      <div>
        <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight text-neutral-900 dark:text-white">
          Send Money
        </h1>
        <p className="text-xs sm:text-sm text-neutral-500 dark:text-neutral-400 mt-1">
          Send money directly to any FinFlow account holder with instant confirmation.
        </p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-12 gap-6">
        {/* Main Transfer Form / Wizard */}
        <div className="md:col-span-8">
          <div className="p-6 sm:p-8 rounded-2xl bg-white dark:bg-[#121620] border border-neutral-200/80 dark:border-neutral-800/80 shadow-sm">
            {error && (
              <div className="mb-6 p-4 rounded-xl bg-rose-50 dark:bg-rose-950/40 border border-rose-200 dark:border-rose-800/40 text-rose-700 dark:text-rose-400 text-xs sm:text-sm flex items-start gap-3">
                <AlertCircle className="w-5 h-5 shrink-0 mt-0.5 text-rose-600 dark:text-rose-400" />
                <div>
                  <p className="font-semibold">Unable to process transfer</p>
                  <p className="mt-0.5">{error}</p>
                </div>
              </div>
            )}

            {step === 'input' && (
              <form onSubmit={handleStartReview} className="space-y-6">
                {/* Available Balance Header */}
                <div className="p-4 rounded-xl bg-neutral-50 dark:bg-neutral-900/60 border border-neutral-200/60 dark:border-neutral-800/60 flex items-center justify-between">
                  <div>
                    <span className="text-xs text-neutral-500 dark:text-neutral-400 block">
                      From Account
                    </span>
                    <span className="text-sm font-semibold font-mono text-neutral-900 dark:text-white">
                      {account ? maskAccountNumber(account.accountNumber) : '•••• ••••'}
                    </span>
                  </div>
                  <div className="text-right">
                    <span className="text-xs text-neutral-500 dark:text-neutral-400 block">
                      Available Balance
                    </span>
                    <span className="text-sm font-bold font-mono text-emerald-600 dark:text-emerald-400">
                      {account ? formatCurrency(account.balance) : '₹0.00'}
                    </span>
                  </div>
                </div>

                {/* Recipient Account Input */}
                <div>
                  <label className="block text-xs font-semibold uppercase tracking-wider text-neutral-700 dark:text-neutral-300 mb-2">
                    Recipient Account Number
                  </label>
                  <input
                    type="text"
                    value={toAccountNumber}
                    onChange={(e) => setToAccountNumber(e.target.value)}
                    placeholder="Enter recipient account number"
                    className="w-full px-4 py-3 text-sm bg-neutral-50 dark:bg-neutral-900/60 border border-neutral-200 dark:border-neutral-800 rounded-xl text-neutral-900 dark:text-white placeholder-neutral-400 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 font-mono"
                    required
                  />
                  <p className="text-[11px] text-neutral-400 dark:text-neutral-500 mt-1">
                    Enter the FinFlow account number of the recipient.
                  </p>
                </div>

                {/* Amount Input */}
                <div>
                  <label className="block text-xs font-semibold uppercase tracking-wider text-neutral-700 dark:text-neutral-300 mb-2">
                    Transfer Amount (INR)
                  </label>
                  <div className="relative">
                    <span className="absolute left-4 top-1/2 -translate-y-1/2 text-xl font-bold text-neutral-400">
                      ₹
                    </span>
                    <input
                      type="number"
                      min="1"
                      step="any"
                      value={amount}
                      onChange={(e) => setAmount(e.target.value)}
                      placeholder="0.00"
                      className="w-full pl-9 pr-4 py-3 text-xl font-bold bg-neutral-50 dark:bg-neutral-900/60 border border-neutral-200 dark:border-neutral-800 rounded-xl text-neutral-900 dark:text-white placeholder-neutral-400 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500"
                      required
                    />
                  </div>
                </div>

                {/* Description (Optional) */}
                <div>
                  <label className="block text-xs font-semibold uppercase tracking-wider text-neutral-700 dark:text-neutral-300 mb-2">
                    Note / Purpose (Optional)
                  </label>
                  <input
                    type="text"
                    value={description}
                    onChange={(e) => setDescription(e.target.value)}
                    placeholder="e.g. Dinner share, Rent, Gift"
                    className="w-full px-4 py-3 text-sm bg-neutral-50 dark:bg-neutral-900/60 border border-neutral-200 dark:border-neutral-800 rounded-xl text-neutral-900 dark:text-white placeholder-neutral-400 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500"
                  />
                </div>

                <button
                  type="submit"
                  className="w-full py-3.5 px-4 bg-blue-600 hover:bg-blue-500 text-white font-semibold rounded-xl transition-all shadow-md shadow-blue-600/20 flex items-center justify-center gap-2"
                >
                  <span>Review Transfer Details</span>
                  <ArrowRight className="w-4 h-4" />
                </button>
              </form>
            )}

            {step === 'review' && (
              <div className="space-y-6">
                <div className="text-center pb-4 border-b border-neutral-100 dark:border-neutral-800">
                  <span className="text-xs uppercase tracking-wider text-neutral-500 dark:text-neutral-400 font-semibold">
                    You are sending
                  </span>
                  <div className="text-4xl font-extrabold text-neutral-900 dark:text-white font-mono mt-1">
                    {formatCurrency(parseFloat(amount))}
                  </div>
                </div>

                <div className="space-y-3 p-4 rounded-xl bg-neutral-50 dark:bg-neutral-900/60 border border-neutral-200/60 dark:border-neutral-800/60 text-xs sm:text-sm">
                  <div className="flex justify-between">
                    <span className="text-neutral-500 dark:text-neutral-400">From Account:</span>
                    <span className="font-mono font-medium text-neutral-900 dark:text-white">
                      {account ? account.accountNumber : '—'}
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-neutral-500 dark:text-neutral-400">To Account:</span>
                    <span className="font-mono font-bold text-neutral-900 dark:text-white">
                      {toAccountNumber}
                    </span>
                  </div>
                  {description && (
                    <div className="flex justify-between">
                      <span className="text-neutral-500 dark:text-neutral-400">Note:</span>
                      <span className="text-neutral-900 dark:text-white">{description}</span>
                    </div>
                  )}
                  <div className="flex justify-between pt-2 border-t border-neutral-200/60 dark:border-neutral-800/60">
                    <span className="text-neutral-500 dark:text-neutral-400">Security Check:</span>
                    <span className="text-emerald-600 dark:text-emerald-400 font-semibold flex items-center gap-1">
                      <ShieldCheck className="w-3.5 h-3.5" />
                      Encrypted & Protected
                    </span>
                  </div>
                </div>

                <div className="flex items-center gap-3">
                  <button
                    type="button"
                    onClick={() => setStep('input')}
                    disabled={isLoading}
                    className="flex-1 py-3 px-4 bg-neutral-100 hover:bg-neutral-200 dark:bg-neutral-800 dark:hover:bg-neutral-700 text-neutral-700 dark:text-neutral-200 font-medium rounded-xl transition-all"
                  >
                    Back to Edit
                  </button>
                  <button
                    type="button"
                    onClick={handleExecuteTransfer}
                    disabled={isLoading}
                    className="flex-1 py-3 px-4 bg-blue-600 hover:bg-blue-500 disabled:opacity-50 text-white font-semibold rounded-xl transition-all shadow-md shadow-blue-600/20 flex items-center justify-center gap-2"
                  >
                    {isLoading ? (
                      <>
                        <Loader2 className="w-4 h-4 animate-spin" />
                        <span>Sending transfer...</span>
                      </>
                    ) : (
                      <>
                        <Send className="w-4 h-4" />
                        <span>Confirm & Send</span>
                      </>
                    )}
                  </button>
                </div>
              </div>
            )}

            {step === 'success' && (
              <div className="text-center py-6 space-y-5">
                <div className="w-16 h-16 rounded-full bg-emerald-50 dark:bg-emerald-950/60 text-emerald-600 dark:text-emerald-400 mx-auto flex items-center justify-center border border-emerald-200 dark:border-emerald-800">
                  <CheckCircle2 className="w-8 h-8" />
                </div>
                <div>
                  <h3 className="text-2xl font-bold text-neutral-900 dark:text-white">
                    Transfer Successful!
                  </h3>
                  <p className="text-sm text-neutral-500 dark:text-neutral-400 mt-1">
                    {formatCurrency(parseFloat(amount))} has been sent to{' '}
                    <span className="font-mono text-neutral-800 dark:text-neutral-200 font-semibold">
                      {toAccountNumber}
                    </span>
                  </p>
                </div>

                <div className="p-4 rounded-xl bg-neutral-50 dark:bg-neutral-900/60 border border-neutral-200/60 dark:border-neutral-800/60 max-w-sm mx-auto text-left text-xs space-y-1.5">
                  <div className="flex justify-between">
                    <span className="text-neutral-500">Status:</span>
                    <span className="text-emerald-600 dark:text-emerald-400 font-semibold">
                      Confirmed & Completed
                    </span>
                  </div>
                </div>

                <button
                  onClick={handleReset}
                  className="w-full py-3 px-4 bg-blue-600 hover:bg-blue-500 text-white font-semibold rounded-xl transition-all shadow-sm"
                >
                  Make Another Transfer
                </button>
              </div>
            )}
          </div>
        </div>

        {/* Sidebar Help & Information */}
        <div className="md:col-span-4 space-y-4">
          <div className="p-5 rounded-2xl bg-white dark:bg-[#121620] border border-neutral-200/80 dark:border-neutral-800/80 shadow-sm space-y-3">
            <div className="flex items-center gap-2 text-neutral-900 dark:text-white font-semibold text-sm">
              <ShieldCheck className="w-4 h-4 text-emerald-500" />
              <span>Duplicate Transfer Protection</span>
            </div>
            <p className="text-xs text-neutral-500 dark:text-neutral-400 leading-relaxed">
              Every FinFlow transfer is safeguarded. Even if your internet connection is interrupted while sending, funds will never be deducted twice.
            </p>
          </div>

          <div className="p-5 rounded-2xl bg-blue-50/50 dark:bg-blue-950/20 border border-blue-200/50 dark:border-blue-900/40 text-xs text-blue-900 dark:text-blue-300 space-y-2">
            <div className="flex items-center gap-1.5 font-semibold">
              <Info className="w-4 h-4 text-blue-500" />
              <span>Instant Availability</span>
            </div>
            <p className="leading-relaxed text-blue-800/90 dark:text-blue-300/80">
              Transferred funds are credited to the recipient immediately and available for spending or further transfer.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};
