import React, { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { X, ArrowDownRight, CheckCircle2, AlertCircle, Loader2 } from 'lucide-react';
import { accountsApi, generateIdempotencyKey } from '../api/client';
import { formatCurrency } from '../utils/formatters';
import { useToast } from '../context/ToastContext';

interface DepositModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
}

const PRESET_AMOUNTS = [500, 1000, 2000, 5000, 10000];

export const DepositModal: React.FC<DepositModalProps> = ({
  isOpen,
  onClose,
  onSuccess,
}) => {
  const { showToast } = useToast();
  const [amount, setAmount] = useState<string>('1000');
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [isSuccess, setIsSuccess] = useState<boolean>(false);
  const [lastDepositAmount, setLastDepositAmount] = useState<number>(0);

  const resetForm = () => {
    setAmount('1000');
    setError(null);
    setIsSuccess(false);
  };

  const handleClose = () => {
    if (isLoading) return;
    resetForm();
    onClose();
  };

  const handleDeposit = async (e: React.FormEvent) => {
    e.preventDefault();
    const numAmount = parseFloat(amount);
    if (isNaN(numAmount) || numAmount <= 0) {
      setError('Please enter a valid deposit amount greater than ₹0');
      return;
    }
    if (numAmount > 1000000) {
      setError('Single deposit limit is ₹10,00,000');
      return;
    }

    setIsLoading(true);
    setError(null);

    const idempotencyKey = generateIdempotencyKey();
    try {
      try {
        await accountsApi.deposit({ amount: numAmount }, idempotencyKey);
      } catch (depErr: unknown) {
        const depMsg = depErr instanceof Error ? depErr.message : String(depErr);
        if (depMsg.includes('not found') || depMsg.includes('404')) {
          await accountsApi.createAccount();
          await accountsApi.deposit({ amount: numAmount }, idempotencyKey);
        } else {
          throw depErr;
        }
      }
      setLastDepositAmount(numAmount);
      setIsSuccess(true);
      showToast('Deposit completed successfully', 'success');
      onSuccess();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Deposit failed. Please try again.';
      setError(msg);
      showToast(msg, 'error');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <AnimatePresence>
      {isOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={handleClose}
            className="fixed inset-0 bg-black/60 backdrop-blur-sm"
          />

          <motion.div
            initial={{ opacity: 0, scale: 0.95, y: 15 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.95, y: 15 }}
            transition={{ duration: 0.2 }}
            onClick={(e) => e.stopPropagation()}
            className="relative w-full max-w-md bg-white dark:bg-[#121620] border border-neutral-200 dark:border-neutral-800 rounded-2xl shadow-2xl max-h-[90vh] overflow-y-auto z-10"
          >
            {/* Header */}
            <div className="flex items-center justify-between p-6 border-b border-neutral-100 dark:border-neutral-800">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-xl bg-emerald-50 dark:bg-emerald-950/40 text-emerald-600 dark:text-emerald-400 flex items-center justify-center border border-emerald-100 dark:border-emerald-800/40">
                  <ArrowDownRight className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="font-semibold text-neutral-900 dark:text-white">
                    Add Money
                  </h3>
                  <p className="text-xs text-neutral-500 dark:text-neutral-400">
                    Deposit funds to your primary FinFlow account
                  </p>
                </div>
              </div>
              <button
                onClick={handleClose}
                className="p-2 text-neutral-400 hover:text-neutral-600 dark:hover:text-neutral-200 rounded-lg transition-colors"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Body */}
            <div className="p-6">
              {isSuccess ? (
                <div className="text-center py-6 space-y-4">
                  <div className="w-16 h-16 rounded-full bg-emerald-50 dark:bg-emerald-950/50 text-emerald-600 dark:text-emerald-400 mx-auto flex items-center justify-center border border-emerald-200 dark:border-emerald-800">
                    <CheckCircle2 className="w-8 h-8" />
                  </div>
                  <div>
                    <h4 className="text-xl font-bold text-neutral-900 dark:text-white">
                      {formatCurrency(lastDepositAmount)} Added
                    </h4>
                    <p className="text-sm text-neutral-500 dark:text-neutral-400 mt-1">
                      Funds have been credited instantly to your account.
                    </p>
                  </div>
                  <button
                    onClick={handleClose}
                    className="w-full py-3 px-4 bg-emerald-600 hover:bg-emerald-700 text-white font-medium rounded-xl transition-colors shadow-sm"
                  >
                    Done
                  </button>
                </div>
              ) : (
                <form onSubmit={handleDeposit} className="space-y-5">
                  {error && (
                    <div className="p-3 rounded-xl bg-rose-50 dark:bg-rose-950/40 border border-rose-200 dark:border-rose-800/40 text-rose-700 dark:text-rose-400 text-sm flex items-start gap-2.5">
                      <AlertCircle className="w-4 h-4 mt-0.5 shrink-0" />
                      <span>{error}</span>
                    </div>
                  )}

                  <div>
                    <label className="block text-xs font-semibold uppercase tracking-wider text-neutral-500 dark:text-neutral-400 mb-2">
                      Deposit Amount (INR)
                    </label>
                    <div className="relative">
                      <span className="absolute left-4 top-1/2 -translate-y-1/2 text-2xl font-bold text-neutral-400">
                        ₹
                      </span>
                      <input
                        type="number"
                        min="1"
                        step="any"
                        value={amount}
                        onChange={(e) => setAmount(e.target.value)}
                        placeholder="0.00"
                        className="w-full pl-10 pr-4 py-3 text-2xl font-bold bg-neutral-50 dark:bg-neutral-900/60 border border-neutral-200 dark:border-neutral-800 rounded-xl text-neutral-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-emerald-500/30 focus:border-emerald-500"
                        required
                        disabled={isLoading}
                      />
                    </div>
                  </div>

                  {/* Preset amounts */}
                  <div>
                    <span className="text-xs text-neutral-500 dark:text-neutral-400 mb-2 block">
                      Quick select:
                    </span>
                    <div className="grid grid-cols-5 gap-2">
                      {PRESET_AMOUNTS.map((val) => (
                        <button
                          key={val}
                          type="button"
                          onClick={() => setAmount(val.toString())}
                          className={`py-1.5 px-2 text-xs font-semibold rounded-lg border transition-all hover-lift active-press ${
                            amount === val.toString()
                              ? 'bg-emerald-600 text-white border-emerald-600'
                              : 'bg-neutral-50 dark:bg-neutral-800/60 text-neutral-700 dark:text-neutral-300 border-neutral-200 dark:border-neutral-700 hover:border-neutral-300 dark:hover:border-neutral-600'
                          }`}
                        >
                          +{val >= 1000 ? `${val / 1000}k` : val}
                        </button>
                      ))}
                    </div>
                  </div>

                  <div className="pt-2">
                    <button
                      type="submit"
                      disabled={isLoading}
                      className="w-full py-3 px-4 bg-emerald-600 hover:bg-emerald-700 disabled:opacity-50 text-white font-medium rounded-xl transition-all shadow-sm hover-lift active-press flex items-center justify-center gap-2"
                    >
                      {isLoading ? (
                        <>
                          <Loader2 className="w-5 h-5 animate-spin" />
                          <span>Processing Deposit...</span>
                        </>
                      ) : (
                        <span>Confirm Deposit</span>
                      )}
                    </button>
                  </div>
                </form>
              )}
            </div>
          </motion.div>
        </div>
      )}
    </AnimatePresence>
  );
};
