import React, { useState, useEffect, useRef, useCallback } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Sparkles,
  Send,
  Bot,
  User,
  RefreshCw,
  ArrowRight,
  ShieldCheck,
  Receipt
} from 'lucide-react';
import { aiApi } from '../api/client';
import type { AiChatResponse, AiInsightsResponse, FinancialSummary } from '../types';
import { formatCurrency } from '../utils/formatters';

interface ChatMessage {
  id: string;
  sender: 'user' | 'assistant';
  content: string;
  groundedSummary?: FinancialSummary;
  timestamp: string;
}

const SUGGESTED_PROMPTS = [
  "Summarize my recent spending.",
  "What are my biggest financial trends?",
  "How has my cash flow changed?",
  "Give me practical ways to manage my spending.",
];

export const AiAdvisorPage: React.FC = () => {
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [inputQuestion, setInputQuestion] = useState('');
  const [isTyping, setIsTyping] = useState(false);
  const [insights, setInsights] = useState<AiInsightsResponse | null>(null);
  const [isInsightsLoading, setIsInsightsLoading] = useState(false);
  const [insightsError, setInsightsError] = useState<string | null>(null);

  const messagesEndRef = useRef<HTMLDivElement>(null);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages, isTyping]);

  const loadInsights = useCallback(async () => {
    setIsInsightsLoading(true);
    setInsightsError(null);
    try {
      const data = await aiApi.getInsights();
      setInsights(data);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Unable to connect to financial assistant';
      setInsightsError(msg);
    } finally {
      setIsInsightsLoading(false);
    }
  }, []);

  useEffect(() => {
    loadInsights();
  }, [loadInsights]);

  const handleSendMessage = async (text: string) => {
    const question = text.trim();
    if (!question || isTyping) return;

    const userMsg: ChatMessage = {
      id: `u-${Date.now()}`,
      sender: 'user',
      content: question,
      timestamp: new Date().toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit' }),
    };

    setMessages((prev) => [...prev, userMsg]);
    setInputQuestion('');
    setIsTyping(true);

    try {
      const res: AiChatResponse = await aiApi.askQuestion({ question });

      const assistantMsg: ChatMessage = {
        id: `a-${Date.now()}`,
        sender: 'assistant',
        content: res.answer,
        groundedSummary: res.groundedSummary,
        timestamp: new Date().toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit' }),
      };

      setMessages((prev) => [...prev, assistantMsg]);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Assistant could not process your question at this moment.';
      const errorMsg: ChatMessage = {
        id: `e-${Date.now()}`,
        sender: 'assistant',
        content: `I am currently unable to fetch insights. Please check back shortly or try a different question.`,
        timestamp: new Date().toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit' }),
      };
      setMessages((prev) => [...prev, errorMsg]);
    } finally {
      setIsTyping(false);
    }
  };

  return (
    <div className="space-y-6 animate-fade-in max-w-5xl mx-auto">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight text-neutral-900 dark:text-white">
              Financial Assistant
            </h1>
            <span className="text-xs font-bold px-2 py-0.5 rounded-full bg-blue-100 dark:bg-blue-950/80 text-blue-700 dark:text-blue-300 border border-blue-200 dark:border-blue-800">
              Personalized Guidance
            </span>
          </div>
          <p className="text-xs sm:text-sm text-neutral-500 dark:text-neutral-400 mt-1">
            Get clear answers about your spending trends, recent expenses, and everyday budgeting suggestions.
          </p>
        </div>

        <button
          onClick={loadInsights}
          disabled={isInsightsLoading}
          className="self-start sm:self-auto flex items-center gap-1.5 px-3 py-2 rounded-xl bg-white dark:bg-neutral-800 border border-neutral-200 dark:border-neutral-700 text-xs font-semibold text-neutral-700 dark:text-neutral-200 hover:bg-neutral-50 dark:hover:bg-neutral-700 transition-all disabled:opacity-50"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${isInsightsLoading ? 'animate-spin' : ''}`} />
          <span>Refresh Analysis</span>
        </button>
      </div>

      {/* Top Banner: Insights Overview Card */}
      {insights && (
        <div className="p-6 rounded-2xl bg-gradient-to-br from-indigo-900/10 via-purple-900/5 to-blue-900/10 dark:from-indigo-950/40 dark:via-purple-950/20 dark:to-blue-950/30 border border-indigo-200/50 dark:border-indigo-800/40 shadow-sm space-y-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Sparkles className="w-5 h-5 text-indigo-600 dark:text-indigo-400" />
              <h2 className="font-semibold text-neutral-900 dark:text-white text-sm sm:text-base">
                Monthly Spending & Habit Summary
              </h2>
            </div>
            {insights.summary && (
              <span className="text-xs font-mono text-neutral-500 dark:text-neutral-400">
                Current Balance: {formatCurrency(insights.summary.currentBalance)}
              </span>
            )}
          </div>

          <p className="text-xs sm:text-sm text-neutral-700 dark:text-neutral-300 leading-relaxed whitespace-pre-line">
            {insights.insights}
          </p>

          {/* Quick Metrics from Summary */}
          {insights.summary && (
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 pt-2">
              <div className="bg-white/80 dark:bg-neutral-900/80 p-3 rounded-xl border border-neutral-200/60 dark:border-neutral-800/60">
                <span className="text-[11px] text-neutral-500 dark:text-neutral-400 block uppercase">
                  Money In
                </span>
                <span className="text-sm font-bold text-emerald-600 dark:text-emerald-400 font-mono">
                  {formatCurrency(insights.summary.totalInflow)}
                </span>
              </div>
              <div className="bg-white/80 dark:bg-neutral-900/80 p-3 rounded-xl border border-neutral-200/60 dark:border-neutral-800/60">
                <span className="text-[11px] text-neutral-500 dark:text-neutral-400 block uppercase">
                  Money Out
                </span>
                <span className="text-sm font-bold text-rose-600 dark:text-rose-400 font-mono">
                  {formatCurrency(insights.summary.totalOutflow)}
                </span>
              </div>
              <div className="bg-white/80 dark:bg-neutral-900/80 p-3 rounded-xl border border-neutral-200/60 dark:border-neutral-800/60">
                <span className="text-[11px] text-neutral-500 dark:text-neutral-400 block uppercase">
                  Net Cash Flow
                </span>
                <span className="text-sm font-bold text-blue-600 dark:text-blue-400 font-mono">
                  {formatCurrency(insights.summary.netCashFlow)}
                </span>
              </div>
              <div className="bg-white/80 dark:bg-neutral-900/80 p-3 rounded-xl border border-neutral-200/60 dark:border-neutral-800/60">
                <span className="text-[11px] text-neutral-500 dark:text-neutral-400 block uppercase">
                  Transactions
                </span>
                <span className="text-sm font-bold text-neutral-800 dark:text-neutral-200 font-mono">
                  {insights.summary.completedTransactionCount} Recorded
                </span>
              </div>
            </div>
          )}
        </div>
      )}

      {/* Chat Interface Container */}
      <div className="rounded-2xl bg-white dark:bg-[#121620] border border-neutral-200/80 dark:border-neutral-800/80 shadow-sm flex flex-col h-[580px] overflow-hidden">
        {/* Messages scroll area */}
        <div className="flex-1 overflow-y-auto p-4 sm:p-6 space-y-4">
          {/* Welcome Intro if empty */}
          {messages.length === 0 && (
            <div className="h-full flex flex-col items-center justify-center text-center p-6 space-y-4 max-w-md mx-auto">
              <div className="w-14 h-14 rounded-2xl bg-blue-50 dark:bg-blue-950/60 text-blue-600 dark:text-blue-400 flex items-center justify-center border border-blue-200 dark:border-blue-800">
                <Bot className="w-7 h-7" />
              </div>
              <div>
                <h3 className="text-base font-semibold text-neutral-900 dark:text-white">
                  How can I assist your finances today?
                </h3>
                <p className="text-xs text-neutral-500 dark:text-neutral-400 mt-1 leading-relaxed">
                  Ask me about your spending trends, recent expenses, or tips for keeping your monthly budget on target.
                </p>
              </div>

              {/* Prompt chips */}
              <div className="w-full space-y-2 pt-2">
                <span className="text-[11px] uppercase tracking-wider text-neutral-400 font-semibold block text-left">
                  Suggested inquiries:
                </span>
                <div className="flex flex-col gap-1.5">
                  {SUGGESTED_PROMPTS.map((prompt) => (
                    <button
                      key={prompt}
                      onClick={() => handleSendMessage(prompt)}
                      className="text-left px-3.5 py-2.5 rounded-xl bg-neutral-50 dark:bg-neutral-900/80 hover:bg-neutral-100 dark:hover:bg-neutral-800 text-xs font-medium text-neutral-700 dark:text-neutral-300 border border-neutral-200/70 dark:border-neutral-800 transition-all flex items-center justify-between group"
                    >
                      <span>{prompt}</span>
                      <ArrowRight className="w-3.5 h-3.5 text-neutral-400 group-hover:text-neutral-700 dark:group-hover:text-white transition-colors" />
                    </button>
                  ))}
                </div>
              </div>
            </div>
          )}

          {/* Chat Messages */}
          <AnimatePresence>
            {messages.map((msg) => (
              <motion.div
                key={msg.id}
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                className={`flex gap-3 ${
                  msg.sender === 'user' ? 'justify-end' : 'justify-start'
                }`}
              >
                {msg.sender === 'assistant' && (
                  <div className="w-8 h-8 rounded-lg bg-blue-50 dark:bg-blue-950/50 text-blue-600 dark:text-blue-400 flex items-center justify-center border border-blue-200 dark:border-blue-800/40 shrink-0 mt-0.5">
                    <Bot className="w-4 h-4" />
                  </div>
                )}

                <div
                  className={`max-w-[85%] sm:max-w-[75%] rounded-2xl p-4 text-xs sm:text-sm space-y-2 ${
                    msg.sender === 'user'
                      ? 'bg-blue-600 text-white rounded-br-none shadow-sm'
                      : 'bg-neutral-100 dark:bg-neutral-900 text-neutral-800 dark:text-neutral-200 rounded-bl-none border border-neutral-200/60 dark:border-neutral-800/80'
                  }`}
                >
                  <p className="leading-relaxed whitespace-pre-line">{msg.content}</p>

                  {/* Summary Card attached to assistant response */}
                  {msg.groundedSummary && (
                    <div className="mt-3 p-3 rounded-xl bg-white/70 dark:bg-black/30 border border-neutral-200/60 dark:border-neutral-800 text-xs space-y-2">
                      <div className="flex items-center justify-between border-b border-neutral-200/60 dark:border-neutral-800 pb-1">
                        <span className="font-semibold text-neutral-700 dark:text-neutral-300 flex items-center gap-1">
                          <Receipt className="w-3.5 h-3.5" />
                          <span>Account Activity Snapshot</span>
                        </span>
                        <span className="font-mono font-bold text-neutral-900 dark:text-white">
                          Balance: {formatCurrency(msg.groundedSummary.currentBalance)}
                        </span>
                      </div>
                      <div className="grid grid-cols-2 gap-2 text-[11px]">
                        <div>
                          <span className="text-neutral-500">Money In:</span>{' '}
                          <span className="font-mono text-emerald-600 dark:text-emerald-400 font-semibold">
                            +{formatCurrency(msg.groundedSummary.totalInflow)}
                          </span>
                        </div>
                        <div>
                          <span className="text-neutral-500">Money Out:</span>{' '}
                          <span className="font-mono text-rose-600 dark:text-rose-400 font-semibold">
                            -{formatCurrency(msg.groundedSummary.totalOutflow)}
                          </span>
                        </div>
                      </div>
                    </div>
                  )}

                  <span
                    className={`block text-[10px] text-right mt-1 ${
                      msg.sender === 'user' ? 'text-blue-200' : 'text-neutral-400'
                    }`}
                  >
                    {msg.timestamp}
                  </span>
                </div>

                {msg.sender === 'user' && (
                  <div className="w-8 h-8 rounded-lg bg-blue-100 dark:bg-blue-950 text-blue-600 dark:text-blue-400 flex items-center justify-center shrink-0 mt-0.5">
                    <User className="w-4 h-4" />
                  </div>
                )}
              </motion.div>
            ))}

            {isTyping && (
              <motion.div
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                className="flex items-center gap-3"
              >
                <div className="w-8 h-8 rounded-lg bg-blue-50 dark:bg-blue-950/50 text-blue-600 dark:text-blue-400 flex items-center justify-center border border-blue-200 dark:border-blue-800/40">
                  <Bot className="w-4 h-4" />
                </div>
                <div className="p-3 rounded-2xl bg-neutral-100 dark:bg-neutral-900 text-neutral-500 dark:text-neutral-400 flex items-center gap-1.5 text-xs">
                  <span className="w-2 h-2 rounded-full bg-blue-500 animate-bounce" />
                  <span className="w-2 h-2 rounded-full bg-blue-500 animate-bounce [animation-delay:0.2s]" />
                  <span className="w-2 h-2 rounded-full bg-blue-500 animate-bounce [animation-delay:0.4s]" />
                  <span className="ml-2 font-medium">Analyzing your financial activity...</span>
                </div>
              </motion.div>
            )}
          </AnimatePresence>
          <div ref={messagesEndRef} />
        </div>

        {/* Input Bar */}
        <div className="p-4 border-t border-neutral-100 dark:border-neutral-800 bg-neutral-50/50 dark:bg-neutral-900/30">
          <form
            onSubmit={(e) => {
              e.preventDefault();
              handleSendMessage(inputQuestion);
            }}
            className="flex items-center gap-2"
          >
            <input
              type="text"
              value={inputQuestion}
              onChange={(e) => setInputQuestion(e.target.value)}
              placeholder="Ask about your spending, recent transfers, or budgeting suggestions..."
              disabled={isTyping}
              className="flex-1 px-4 py-2.5 text-xs sm:text-sm bg-white dark:bg-[#121620] border border-neutral-200 dark:border-neutral-800 rounded-xl text-neutral-900 dark:text-white placeholder-neutral-400 focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 disabled:opacity-50"
            />
            <button
              type="submit"
              disabled={!inputQuestion.trim() || isTyping}
              className="px-4 py-2.5 bg-blue-600 hover:bg-blue-500 disabled:opacity-50 text-white rounded-xl font-medium text-xs sm:text-sm transition-all shadow-xs flex items-center gap-1.5"
            >
              <Send className="w-4 h-4" />
              <span className="hidden sm:inline">Ask</span>
            </button>
          </form>

          <div className="mt-2.5 flex items-center justify-between text-[11px] text-neutral-400 dark:text-neutral-500">
            <span className="flex items-center gap-1">
              <ShieldCheck className="w-3.5 h-3.5 text-emerald-500" />
              Grounded in your real account activity
            </span>
            <span>Informational financial companion</span>
          </div>
        </div>
      </div>
    </div>
  );
};
