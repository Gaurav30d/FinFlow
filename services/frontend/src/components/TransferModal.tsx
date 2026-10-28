import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { X, Send, ArrowRight, CheckCircle2, AlertTriangle, ShieldCheck, RefreshCw } from 'lucide-react';
import { api, generateIdempotencyKey } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import { formatCurrency } from '../utils/formatters';

interface TransferModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess?: () => void;
}

type Step = 'FORM' | 'REVIEW' | 'SUCCESS';

export const TransferModal: React.FC<TransferModalProps> = ({ isOpen, onClose, onSuccess }) => {
  const { account, refreshAccount } = useAuth();
  const toast = useToast();

  const [step, setStep] = useState<Step>('FORM');
  const [receiverAccount, setReceiverAccount] = useState('');
  const [amount, setAmount] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [idempotencyKey, setIdempotencyKey] = useState<string>('');
  const [completedTxRef, setCompletedTxRef] = useState<string>('');

  useEffect(() => {
    if (isOpen) {
      setStep('FORM');
      setReceiverAccount('');
      setAmount('');
      setErrorMsg(null);
      setIdempotencyKey(generateIdempotencyKey());
    }
  }, [isOpen]);

  if (!isOpen) return null;

  const currentBalance = account?.balance ?? 0;
  const numAmount = parseFloat(amount);

  const handleValidateForm = (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg(null);

    const cleanReceiver = receiverAccount.trim();
    if (!cleanReceiver) {
      setErrorMsg('Recipient account number is required.');
      return;
    }

    if (account?.accountNumber && cleanReceiver === account.accountNumber) {
      setErrorMsg('Self-transfers are not allowed. Please enter another account number.');
      return;
    }

    if (isNaN(numAmount) || numAmount <= 0) {
      setErrorMsg('Please enter a valid amount greater than zero.');
      return;
    }

    if (numAmount > currentBalance) {
      setErrorMsg(`Insufficient balance. Your available balance is ${formatCurrency(currentBalance)}.`);
      return;
    }

    setStep('REVIEW');
  };

  const handleExecuteTransfer = async () => {
    setIsLoading(true);
    setErrorMsg(null);

    try {
      const tx = await api.transactions.transfer(
        receiverAccount.trim(),
        numAmount,
        account?.currency || 'INR',
        idempotencyKey
      );

      setCompletedTxRef(tx.transactionReference);
      setStep('SUCCESS');
      toast.success(`Successfully transferred ${formatCurrency(numAmount)}!`);
      await refreshAccount();
      if (onSuccess) onSuccess();
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Transfer failed. Please verify recipient details and try again.';
      setErrorMsg(message);
      toast.error(message);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm">
      <motion.div
        initial={{ opacity: 0, scale: 0.95, y: 10 }}
        animate={{ opacity: 1, scale: 1, y: 0 }}
        exit={{ opacity: 0, scale: 0.95, y: 10 }}
        onClick={(e) => e.stopPropagation()}
        className="w-full max-w-lg bg-white dark:bg-slate-900 rounded-3xl shadow-2xl border border-slate-200 dark:border-slate-800 max-h-[90vh] overflow-y-auto"
      >
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-5 border-b border-slate-200 dark:border-slate-800">
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-xl bg-indigo-50 dark:bg-indigo-950/60 flex items-center justify-center text-indigo-600 dark:text-indigo-400">
              <Send className="w-4 h-4" />
            </div>
            <div>
              <h2 className="text-base font-bold text-slate-900 dark:text-white">
                {step === 'SUCCESS' ? 'Transfer Completed' : 'Send Money'}
              </h2>
              <p className="text-xs text-slate-500">
                {step === 'FORM'
                  ? 'Transfer funds securely to any FinFlow account'
                  : step === 'REVIEW'
                  ? 'Verify transfer details'
                  : 'Transaction processed successfully'}
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 rounded-lg text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content based on Step */}
        <div className="p-6">
          <AnimatePresence mode="wait">
            {step === 'FORM' && (
              <motion.form
                key="form"
                initial={{ opacity: 0, x: -10 }}
                animate={{ opacity: 1, x: 0 }}
                exit={{ opacity: 0, x: 10 }}
                onSubmit={handleValidateForm}
                className="space-y-4"
              >
                {/* Available Balance pill */}
                <div className="flex items-center justify-between p-3.5 rounded-xl bg-slate-50 dark:bg-slate-800/40 border border-slate-200/60 dark:border-slate-800/60 text-xs">
                  <span className="text-slate-500 font-medium">Available to send:</span>
                  <span className="font-bold text-slate-800 dark:text-slate-100">
                    {formatCurrency(currentBalance, account?.currency)}
                  </span>
                </div>

                {/* Recipient Account Input */}
                <div>
                  <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 uppercase tracking-wider mb-2">
                    Recipient Account Number
                  </label>
                  <input
                    type="text"
                    required
                    placeholder="e.g. 8fa19c2e-4b21-4f18..."
                    value={receiverAccount}
                    onChange={(e) => setReceiverAccount(e.target.value)}
                    className="w-full px-4 py-3 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800/80 text-slate-900 dark:text-white placeholder-slate-400 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 transition-all"
                  />
                  <p className="text-[11px] text-slate-400 mt-1">
                    Enter the exact FinFlow account number of the recipient.
                  </p>
                </div>

                {/* Amount Input */}
                <div>
                  <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 uppercase tracking-wider mb-2">
                    Amount (INR)
                  </label>
                  <div className="relative">
                    <span className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-400 font-semibold text-base">
                      ₹
                    </span>
                    <input
                      type="number"
                      step="0.01"
                      min="1"
                      required
                      placeholder="0.00"
                      value={amount}
                      onChange={(e) => setAmount(e.target.value)}
                      className="w-full pl-9 pr-4 py-3 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800/80 text-slate-900 dark:text-white placeholder-slate-400 text-base font-semibold focus:outline-none focus:ring-2 focus:ring-indigo-500 transition-all"
                    />
                  </div>
                </div>

                {errorMsg && (
                  <div className="flex items-start gap-2.5 p-3.5 rounded-xl bg-rose-50 dark:bg-rose-950/40 border border-rose-200 dark:border-rose-900 text-rose-700 dark:text-rose-300 text-xs leading-relaxed">
                    <AlertTriangle className="w-4 h-4 shrink-0 mt-0.5" />
                    <span>{errorMsg}</span>
                  </div>
                )}

                <div className="pt-2 flex justify-end gap-3">
                  <button
                    type="button"
                    onClick={onClose}
                    className="px-4 py-2.5 rounded-xl border border-slate-200 dark:border-slate-700 text-slate-600 dark:text-slate-300 text-sm font-medium hover:bg-slate-50 dark:hover:bg-slate-800 transition-colors"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    className="px-5 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white text-sm font-medium shadow-md shadow-indigo-600/20 active:scale-95 transition-all flex items-center gap-2"
                  >
                    <span>Review Transfer</span>
                    <ArrowRight className="w-4 h-4" />
                  </button>
                </div>
              </motion.form>
            )}

            {step === 'REVIEW' && (
              <motion.div
                key="review"
                initial={{ opacity: 0, x: 10 }}
                animate={{ opacity: 1, x: 0 }}
                exit={{ opacity: 0, x: -10 }}
                className="space-y-4"
              >
                <div className="p-4 rounded-2xl bg-slate-50 dark:bg-slate-800/50 border border-slate-200/80 dark:border-slate-700/80 space-y-3">
                  <div className="flex justify-between items-center text-sm border-b border-slate-200/60 dark:border-slate-700/60 pb-2.5">
                    <span className="text-slate-500">Transfer Amount</span>
                    <span className="text-lg font-bold text-slate-900 dark:text-white">
                      {formatCurrency(numAmount)}
                    </span>
                  </div>
                  <div className="flex justify-between items-center text-xs border-b border-slate-200/60 dark:border-slate-700/60 pb-2.5">
                    <span className="text-slate-500">From Account</span>
                    <span className="font-mono text-slate-700 dark:text-slate-300">
                      {account?.accountNumber || 'Primary Account'}
                    </span>
                  </div>
                  <div className="flex justify-between items-center text-xs border-b border-slate-200/60 dark:border-slate-700/60 pb-2.5">
                    <span className="text-slate-500">To Account</span>
                    <span className="font-mono text-indigo-600 dark:text-indigo-400 font-semibold truncate max-w-[220px]">
                      {receiverAccount}
                    </span>
                  </div>
                  <div className="flex justify-between items-center text-xs">
                    <span className="text-slate-500">Protection Check</span>
                    <span className="text-emerald-600 dark:text-emerald-400 font-semibold flex items-center gap-1">
                      <ShieldCheck className="w-3.5 h-3.5" />
                      Verified
                    </span>
                  </div>
                </div>

                <div className="flex items-center gap-2 text-xs text-emerald-600 dark:text-emerald-400 bg-emerald-50 dark:bg-emerald-950/30 p-3 rounded-xl border border-emerald-200/60 dark:border-emerald-900/60">
                  <ShieldCheck className="w-4 h-4 shrink-0" />
                  <span>Protected against duplicate transfers with instant confirmation.</span>
                </div>

                {errorMsg && (
                  <div className="flex items-start gap-2.5 p-3.5 rounded-xl bg-rose-50 dark:bg-rose-950/40 border border-rose-200 dark:border-rose-900 text-rose-700 dark:text-rose-300 text-xs">
                    <AlertTriangle className="w-4 h-4 shrink-0 mt-0.5" />
                    <span>{errorMsg}</span>
                  </div>
                )}

                <div className="pt-2 flex justify-end gap-3">
                  <button
                    type="button"
                    disabled={isLoading}
                    onClick={() => setStep('FORM')}
                    className="px-4 py-2.5 rounded-xl border border-slate-200 dark:border-slate-700 text-slate-600 dark:text-slate-300 text-sm font-medium hover:bg-slate-50 dark:hover:bg-slate-800 transition-colors"
                  >
                    Back
                  </button>
                  <button
                    type="button"
                    disabled={isLoading}
                    onClick={handleExecuteTransfer}
                    className="px-6 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-500 disabled:opacity-50 text-white text-sm font-medium shadow-md shadow-indigo-600/20 active:scale-95 transition-all flex items-center gap-2"
                  >
                    {isLoading ? (
                      <>
                        <RefreshCw className="w-4 h-4 animate-spin" />
                        <span>Processing Transfer...</span>
                      </>
                    ) : (
                      <>
                        <Send className="w-4 h-4" />
                        <span>Confirm & Send</span>
                      </>
                    )}
                  </button>
                </div>
              </motion.div>
            )}

            {step === 'SUCCESS' && (
              <motion.div
                key="success"
                initial={{ opacity: 0, scale: 0.95 }}
                animate={{ opacity: 1, scale: 1 }}
                className="text-center py-4 space-y-4"
              >
                <div className="w-16 h-16 rounded-full bg-emerald-100 dark:bg-emerald-950/60 text-emerald-600 dark:text-emerald-400 mx-auto flex items-center justify-center">
                  <CheckCircle2 className="w-8 h-8" />
                </div>
                <div>
                  <h3 className="text-xl font-bold text-slate-900 dark:text-white">
                    {formatCurrency(numAmount)} Sent
                  </h3>
                  <p className="text-xs text-slate-500 mt-1">
                    Your transfer has been verified and settled across accounts.
                  </p>
                </div>

                {completedTxRef && (
                  <div className="p-3 bg-slate-50 dark:bg-slate-800/40 rounded-xl border border-slate-200 dark:border-slate-800 text-xs font-mono text-slate-600 dark:text-slate-300">
                    Ref: {completedTxRef}
                  </div>
                )}

                <div className="pt-3">
                  <button
                    onClick={onClose}
                    className="w-full py-2.5 rounded-xl bg-slate-900 dark:bg-white text-white dark:text-slate-900 font-semibold text-sm hover:opacity-90 transition-opacity"
                  >
                    Done
                  </button>
                </div>
              </motion.div>
            )}
          </AnimatePresence>
        </div>
      </motion.div>
    </div>
  );
};
