import React from 'react';
import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import {
  ArrowRight,
  ShieldCheck,
  Send,
  Sparkles,
  CreditCard,
  CheckCircle2,
  ArrowDownLeft,
  ArrowUpRight,
  Lock,
  TrendingUp,
  Clock,
  Eye,
  HelpCircle,
  Smartphone
} from 'lucide-react';
import { ThemeToggle } from '../components/ThemeToggle';

export const LandingPage: React.FC = () => {
  return (
    <div className="min-h-screen bg-neutral-50 dark:bg-[#0a0d14] text-neutral-900 dark:text-neutral-100 flex flex-col selection:bg-blue-600 selection:text-white transition-colors duration-200">
      {/* Top Navigation */}
      <header className="sticky top-0 z-50 backdrop-blur-md bg-white/80 dark:bg-[#0a0d14]/80 border-b border-neutral-200/70 dark:border-neutral-800/70">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
          {/* Brand */}
          <Link to="/" className="flex items-center gap-2.5 group">
            <div className="w-9 h-9 rounded-xl bg-gradient-to-tr from-blue-600 to-indigo-600 flex items-center justify-center font-bold text-white shadow-md shadow-blue-500/20 group-hover:scale-105 transition-transform">
              F
            </div>
            <span className="font-extrabold text-xl tracking-tight text-neutral-900 dark:text-white">
              FinFlow
            </span>
          </Link>

          {/* Nav links */}
          <nav className="hidden md:flex items-center gap-8 text-sm font-medium text-neutral-600 dark:text-neutral-300">
            <a href="#features" className="hover:text-blue-600 dark:hover:text-blue-400 transition-colors">
              Features
            </a>
            <a href="#how-it-works" className="hover:text-blue-600 dark:hover:text-blue-400 transition-colors">
              How It Works
            </a>
            <a href="#security" className="hover:text-blue-600 dark:hover:text-blue-400 transition-colors">
              Security
            </a>
          </nav>

          {/* Action buttons & Theme Toggle */}
          <div className="flex items-center gap-3">
            <ThemeToggle />
            <Link
              to="/login"
              className="hidden sm:inline-flex px-4 py-2 text-sm font-semibold text-neutral-700 dark:text-neutral-200 hover:text-neutral-900 dark:hover:text-white transition-colors"
            >
              Sign In
            </Link>
            <Link
              to="/register"
              className="px-4 py-2 text-sm font-semibold text-white bg-blue-600 hover:bg-blue-500 rounded-xl shadow-sm hover:shadow-md transition-all active:scale-95"
            >
              Open an Account
            </Link>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="flex-1">
        {/* Hero Section */}
        <section className="relative overflow-hidden pt-12 pb-20 sm:pt-20 sm:pb-28">
          {/* Subtle Ambient Background Lighting */}
          <div className="absolute top-1/4 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[600px] h-[300px] bg-gradient-to-tr from-blue-500/10 via-indigo-500/10 to-transparent blur-3xl rounded-full pointer-events-none" />

          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 relative z-10">
            <div className="text-center max-w-3xl mx-auto space-y-6">
              {/* Product Badge */}
              <motion.div
                initial={{ opacity: 0, y: 15 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.3 }}
                className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-blue-50 dark:bg-blue-950/60 border border-blue-200/80 dark:border-blue-800/60 text-xs font-semibold text-blue-700 dark:text-blue-300 shadow-xs"
              >
                <Sparkles className="w-3.5 h-3.5 text-blue-500" />
                <span>Next-Generation Everyday Digital Banking</span>
              </motion.div>

              {/* Headline */}
              <motion.h1
                initial={{ opacity: 0, y: 15 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.4, delay: 0.1 }}
                className="text-4xl sm:text-6xl font-extrabold tracking-tight text-neutral-900 dark:text-white leading-[1.12]"
              >
                Your money. Your goals. <br />
                <span className="bg-gradient-to-r from-blue-600 to-indigo-600 dark:from-blue-400 dark:to-indigo-400 bg-clip-text text-transparent">
                  One smarter place.
                </span>
              </motion.h1>

              {/* Supporting Text */}
              <motion.p
                initial={{ opacity: 0, y: 15 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.4, delay: 0.2 }}
                className="text-base sm:text-xl text-neutral-600 dark:text-neutral-300 leading-relaxed max-w-2xl mx-auto"
              >
                Manage your money, make transfers, track your spending, and understand your financial habits with FinFlow.
              </motion.p>

              {/* CTAs */}
              <motion.div
                initial={{ opacity: 0, y: 15 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.4, delay: 0.3 }}
                className="flex flex-col sm:flex-row items-center justify-center gap-3 pt-2"
              >
                <Link
                  to="/register"
                  className="w-full sm:w-auto px-7 py-3.5 bg-blue-600 hover:bg-blue-500 text-white font-semibold rounded-xl text-base shadow-lg shadow-blue-600/25 transition-all flex items-center justify-center gap-2 group active:scale-95"
                >
                  <span>Open an Account</span>
                  <ArrowRight className="w-4 h-4 group-hover:translate-x-1 transition-transform" />
                </Link>
                <Link
                  to="/login"
                  className="w-full sm:w-auto px-7 py-3.5 bg-white dark:bg-[#121620] hover:bg-neutral-100 dark:hover:bg-neutral-800 text-neutral-800 dark:text-neutral-200 border border-neutral-300 dark:border-neutral-700 font-semibold rounded-xl text-base transition-all flex items-center justify-center active:scale-95 shadow-xs"
                >
                  Sign In to Account
                </Link>
              </motion.div>

              {/* Trust Badges */}
              <motion.div
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                transition={{ duration: 0.5, delay: 0.4 }}
                className="flex flex-wrap items-center justify-center gap-6 pt-4 text-xs text-neutral-500 dark:text-neutral-400"
              >
                <div className="flex items-center gap-1.5">
                  <CheckCircle2 className="w-4 h-4 text-emerald-500" />
                  <span>Free account opening</span>
                </div>
                <div className="flex items-center gap-1.5">
                  <CheckCircle2 className="w-4 h-4 text-emerald-500" />
                  <span>Instant money transfers</span>
                </div>
                <div className="flex items-center gap-1.5">
                  <CheckCircle2 className="w-4 h-4 text-emerald-500" />
                  <span>Bank-grade security</span>
                </div>
              </motion.div>
            </div>

            {/* Interactive Illustrative Account Preview Mockup */}
            <motion.div
              initial={{ opacity: 0, y: 30 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.6, delay: 0.4 }}
              className="mt-14 max-w-4xl mx-auto"
            >
              <div className="relative rounded-3xl p-1 bg-gradient-to-b from-blue-500/20 via-indigo-500/10 to-transparent shadow-2xl">
                <div className="rounded-[22px] bg-white dark:bg-[#10141e] border border-neutral-200 dark:border-neutral-800 overflow-hidden p-6 sm:p-8">
                  {/* Mockup Header Note */}
                  <div className="flex items-center justify-between pb-6 border-b border-neutral-100 dark:border-neutral-800">
                    <div className="flex items-center gap-3">
                      <div className="w-9 h-9 rounded-xl bg-blue-50 dark:bg-blue-950/60 text-blue-600 dark:text-blue-400 flex items-center justify-center font-bold">
                        <CreditCard className="w-5 h-5" />
                      </div>
                      <div>
                        <div className="flex items-center gap-2">
                          <span className="text-sm font-bold text-neutral-900 dark:text-white">
                            FinFlow Smart Account
                          </span>
                          <span className="text-[10px] font-semibold px-2 py-0.5 rounded-full bg-neutral-100 dark:bg-neutral-800 text-neutral-600 dark:text-neutral-400 border border-neutral-200 dark:border-neutral-700">
                            Illustrative Sample UI
                          </span>
                        </div>
                        <span className="text-xs text-neutral-400">Account ending in •••• 3081</span>
                      </div>
                    </div>
                    <div className="hidden sm:flex items-center gap-2 text-xs font-semibold text-emerald-600 dark:text-emerald-400 bg-emerald-50 dark:bg-emerald-950/40 px-2.5 py-1 rounded-lg border border-emerald-200 dark:border-emerald-800/40">
                      <CheckCircle2 className="w-3.5 h-3.5" />
                      <span>Active & Verified</span>
                    </div>
                  </div>

                  {/* Mockup Body: Balance & Sample Metrics */}
                  <div className="grid grid-cols-1 md:grid-cols-12 gap-6 my-6">
                    <div className="md:col-span-6 p-6 rounded-2xl bg-gradient-to-br from-[#0b101c] to-[#141d30] text-white">
                      <div className="flex items-center justify-between text-xs text-neutral-400">
                        <span>Available Balance</span>
                        <div className="flex items-center gap-1 text-[11px] text-blue-400">
                          <Eye className="w-3.5 h-3.5" />
                          <span>Visible</span>
                        </div>
                      </div>
                      <div className="text-3xl sm:text-4xl font-extrabold font-mono text-white mt-2 mb-1">
                        ₹48,250.00
                      </div>
                      <p className="text-xs text-neutral-400 mb-6">
                        Updated in real time with zero delayed postings
                      </p>
                      <div className="flex items-center gap-2.5">
                        <Link
                          to="/register"
                          className="px-4 py-2 bg-blue-600 hover:bg-blue-500 text-white text-xs font-semibold rounded-lg flex items-center gap-1.5 transition-all shadow-sm"
                        >
                          <Send className="w-3.5 h-3.5" />
                          <span>Send Money</span>
                        </Link>
                        <Link
                          to="/register"
                          className="px-4 py-2 bg-white/10 hover:bg-white/15 text-white text-xs font-semibold rounded-lg transition-all"
                        >
                          Add Funds
                        </Link>
                      </div>
                    </div>

                    <div className="md:col-span-6 flex flex-col justify-between gap-3">
                      <div className="p-4 rounded-xl bg-neutral-50 dark:bg-neutral-900/60 border border-neutral-100 dark:border-neutral-800 flex items-center justify-between">
                        <div className="flex items-center gap-3">
                          <div className="w-9 h-9 rounded-lg bg-emerald-50 dark:bg-emerald-950/40 text-emerald-600 dark:text-emerald-400 flex items-center justify-center border border-emerald-100 dark:border-emerald-900/40">
                            <ArrowDownLeft className="w-4 h-4" />
                          </div>
                          <div>
                            <span className="text-xs text-neutral-500 block">Money In This Month</span>
                            <span className="text-base font-bold font-mono text-neutral-900 dark:text-white">
                              +₹65,000.00
                            </span>
                          </div>
                        </div>
                        <span className="text-xs font-semibold text-emerald-600 dark:text-emerald-400">+14% vs last mo</span>
                      </div>

                      <div className="p-4 rounded-xl bg-neutral-50 dark:bg-neutral-900/60 border border-neutral-100 dark:border-neutral-800 flex items-center justify-between">
                        <div className="flex items-center gap-3">
                          <div className="w-9 h-9 rounded-lg bg-rose-50 dark:bg-rose-950/40 text-rose-600 dark:text-rose-400 flex items-center justify-center border border-rose-100 dark:border-rose-900/40">
                            <ArrowUpRight className="w-4 h-4" />
                          </div>
                          <div>
                            <span className="text-xs text-neutral-500 block">Money Out This Month</span>
                            <span className="text-base font-bold font-mono text-neutral-900 dark:text-white">
                              -₹16,750.00
                            </span>
                          </div>
                        </div>
                        <span className="text-xs font-semibold text-neutral-500">Normal range</span>
                      </div>

                      {/* Mockup AI Highlight */}
                      <div className="p-3.5 rounded-xl bg-indigo-50/70 dark:bg-indigo-950/30 border border-indigo-100 dark:border-indigo-900/40 flex items-start gap-2.5 text-xs text-indigo-900 dark:text-indigo-200">
                        <Sparkles className="w-4 h-4 text-indigo-600 dark:text-indigo-400 shrink-0 mt-0.5" />
                        <div>
                          <span className="font-semibold">FinFlow Smart Tip:</span>
                          <span className="ml-1 text-neutral-600 dark:text-neutral-300">
                            You saved 18% more this week compared to last month. Keep it going!
                          </span>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </motion.div>
          </div>
        </section>

        {/* Customer Benefits Section */}
        <section id="features" className="py-20 bg-white dark:bg-[#0c0f17] border-y border-neutral-200/80 dark:border-neutral-800/80">
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
            <div className="text-center max-w-2xl mx-auto space-y-4 mb-16">
              <span className="text-xs font-bold uppercase tracking-wider text-blue-600 dark:text-blue-400">
                Customer-First Banking
              </span>
              <h2 className="text-3xl sm:text-4xl font-extrabold text-neutral-900 dark:text-white tracking-tight">
                Everything you need to handle your money with confidence
              </h2>
              <p className="text-sm sm:text-base text-neutral-600 dark:text-neutral-300">
                FinFlow is built to make day-to-day finances simple, transparent, and effortlessly organized.
              </p>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
              {/* Feature 1 */}
              <div className="p-6 rounded-2xl bg-neutral-50 dark:bg-[#121620] border border-neutral-200/80 dark:border-neutral-800/80 hover:shadow-md transition-shadow space-y-3">
                <div className="w-10 h-10 rounded-xl bg-blue-100 dark:bg-blue-950/60 text-blue-600 dark:text-blue-400 flex items-center justify-center font-bold">
                  <Smartphone className="w-5 h-5" />
                </div>
                <h3 className="text-base font-bold text-neutral-900 dark:text-white">
                  Effortless Account Management
                </h3>
                <p className="text-xs sm:text-sm text-neutral-600 dark:text-neutral-400 leading-relaxed">
                  View your authoritative balance, add funds, and manage your account details seamlessly from any device.
                </p>
              </div>

              {/* Feature 2 */}
              <div className="p-6 rounded-2xl bg-neutral-50 dark:bg-[#121620] border border-neutral-200/80 dark:border-neutral-800/80 hover:shadow-md transition-shadow space-y-3">
                <div className="w-10 h-10 rounded-xl bg-indigo-100 dark:bg-indigo-950/60 text-indigo-600 dark:text-indigo-400 flex items-center justify-center font-bold">
                  <Send className="w-5 h-5" />
                </div>
                <h3 className="text-base font-bold text-neutral-900 dark:text-white">
                  Instant Money Transfers
                </h3>
                <p className="text-xs sm:text-sm text-neutral-600 dark:text-neutral-400 leading-relaxed">
                  Send money to any FinFlow account holder instantly. Every transaction is confirmed immediately with zero delays.
                </p>
              </div>

              {/* Feature 3 */}
              <div className="p-6 rounded-2xl bg-neutral-50 dark:bg-[#121620] border border-neutral-200/80 dark:border-neutral-800/80 hover:shadow-md transition-shadow space-y-3">
                <div className="w-10 h-10 rounded-xl bg-purple-100 dark:bg-purple-950/60 text-purple-600 dark:text-purple-400 flex items-center justify-center font-bold">
                  <Sparkles className="w-5 h-5" />
                </div>
                <h3 className="text-base font-bold text-neutral-900 dark:text-white">
                  Smart Spending Insights
                </h3>
                <p className="text-xs sm:text-sm text-neutral-600 dark:text-neutral-400 leading-relaxed">
                  Get personalized summaries of where your money goes and ask questions anytime to keep your budget on target.
                </p>
              </div>

              {/* Feature 4 */}
              <div className="p-6 rounded-2xl bg-neutral-50 dark:bg-[#121620] border border-neutral-200/80 dark:border-neutral-800/80 hover:shadow-md transition-shadow space-y-3">
                <div className="w-10 h-10 rounded-xl bg-emerald-100 dark:bg-emerald-950/60 text-emerald-600 dark:text-emerald-400 flex items-center justify-center font-bold">
                  <ShieldCheck className="w-5 h-5" />
                </div>
                <h3 className="text-base font-bold text-neutral-900 dark:text-white">
                  Safe & Secure Always
                </h3>
                <p className="text-xs sm:text-sm text-neutral-600 dark:text-neutral-400 leading-relaxed">
                  Bank-grade encryption, duplicate transfer protection, and real-time security alerts keep your funds guarded.
                </p>
              </div>
            </div>
          </div>
        </section>

        {/* How It Works Section */}
        <section id="how-it-works" className="py-20">
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
            <div className="text-center max-w-2xl mx-auto space-y-4 mb-16">
              <span className="text-xs font-bold uppercase tracking-wider text-blue-600 dark:text-blue-400">
                Simple & Seamless
              </span>
              <h2 className="text-3xl sm:text-4xl font-extrabold text-neutral-900 dark:text-white tracking-tight">
                How FinFlow works for your everyday life
              </h2>
              <p className="text-sm sm:text-base text-neutral-600 dark:text-neutral-300">
                Get started in three easy steps with zero complicated paperwork.
              </p>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-8 relative">
              {/* Step 1 */}
              <div className="p-6 rounded-2xl bg-white dark:bg-[#121620] border border-neutral-200/80 dark:border-neutral-800/80 shadow-xs space-y-4 text-center sm:text-left">
                <div className="w-12 h-12 rounded-2xl bg-blue-50 dark:bg-blue-950/50 text-blue-600 dark:text-blue-400 flex items-center justify-center text-lg font-extrabold mx-auto sm:mx-0">
                  1
                </div>
                <h3 className="text-lg font-bold text-neutral-900 dark:text-white">
                  Open your account in minutes
                </h3>
                <p className="text-xs sm:text-sm text-neutral-600 dark:text-neutral-400 leading-relaxed">
                  Register with your basic details and instantly receive your dedicated FinFlow account number ready for action.
                </p>
              </div>

              {/* Step 2 */}
              <div className="p-6 rounded-2xl bg-white dark:bg-[#121620] border border-neutral-200/80 dark:border-neutral-800/80 shadow-xs space-y-4 text-center sm:text-left">
                <div className="w-12 h-12 rounded-2xl bg-blue-50 dark:bg-blue-950/50 text-blue-600 dark:text-blue-400 flex items-center justify-center text-lg font-extrabold mx-auto sm:mx-0">
                  2
                </div>
                <h3 className="text-lg font-bold text-neutral-900 dark:text-white">
                  Add funds & start transacting
                </h3>
                <p className="text-xs sm:text-sm text-neutral-600 dark:text-neutral-400 leading-relaxed">
                  Deposit money into your account or receive transfers from peers. Your balance updates live in seconds.
                </p>
              </div>

              {/* Step 3 */}
              <div className="p-6 rounded-2xl bg-white dark:bg-[#121620] border border-neutral-200/80 dark:border-neutral-800/80 shadow-xs space-y-4 text-center sm:text-left">
                <div className="w-12 h-12 rounded-2xl bg-blue-50 dark:bg-blue-950/50 text-blue-600 dark:text-blue-400 flex items-center justify-center text-lg font-extrabold mx-auto sm:mx-0">
                  3
                </div>
                <h3 className="text-lg font-bold text-neutral-900 dark:text-white">
                  Understand your habits & grow
                </h3>
                <p className="text-xs sm:text-sm text-neutral-600 dark:text-neutral-400 leading-relaxed">
                  Review cash flow trends and chat with your Financial Assistant to make smarter budgeting choices every month.
                </p>
              </div>
            </div>
          </div>
        </section>

        {/* Security & Trust Section */}
        <section id="security" className="py-20 bg-neutral-100/60 dark:bg-[#0c0f17] border-t border-neutral-200/80 dark:border-neutral-800/80">
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
            <div className="max-w-3xl mx-auto text-center space-y-4 mb-12">
              <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-emerald-50 dark:bg-emerald-950/40 text-emerald-700 dark:text-emerald-300 text-xs font-semibold border border-emerald-200 dark:border-emerald-800/40">
                <Lock className="w-3.5 h-3.5 text-emerald-600 dark:text-emerald-400" />
                <span>Security & Privacy First</span>
              </div>
              <h2 className="text-3xl sm:text-4xl font-extrabold text-neutral-900 dark:text-white tracking-tight">
                Your safety is at the heart of FinFlow
              </h2>
              <p className="text-sm sm:text-base text-neutral-600 dark:text-neutral-300">
                We believe financial freedom begins with total security and peace of mind.
              </p>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-6 max-w-5xl mx-auto">
              <div className="p-6 rounded-2xl bg-white dark:bg-[#121620] border border-neutral-200 dark:border-neutral-800 space-y-3">
                <div className="w-9 h-9 rounded-xl bg-emerald-50 dark:bg-emerald-950/40 text-emerald-600 dark:text-emerald-400 flex items-center justify-center font-bold">
                  <ShieldCheck className="w-5 h-5" />
                </div>
                <h3 className="font-bold text-neutral-900 dark:text-white text-base">
                  Bank-Grade Encryption
                </h3>
                <p className="text-xs sm:text-sm text-neutral-600 dark:text-neutral-400 leading-relaxed">
                  All transactions and personal credentials are encrypted in transit and at rest with modern security standards.
                </p>
              </div>

              <div className="p-6 rounded-2xl bg-white dark:bg-[#121620] border border-neutral-200 dark:border-neutral-800 space-y-3">
                <div className="w-9 h-9 rounded-xl bg-blue-50 dark:bg-blue-950/40 text-blue-600 dark:text-blue-400 flex items-center justify-center font-bold">
                  <Clock className="w-5 h-5" />
                </div>
                <h3 className="font-bold text-neutral-900 dark:text-white text-base">
                  Real-Time Activity Alerts
                </h3>
                <p className="text-xs sm:text-sm text-neutral-600 dark:text-neutral-400 leading-relaxed">
                  Receive immediate alerts whenever a transfer occurs or a deposit is credited, keeping you aware at all times.
                </p>
              </div>

              <div className="p-6 rounded-2xl bg-white dark:bg-[#121620] border border-neutral-200 dark:border-neutral-800 space-y-3">
                <div className="w-9 h-9 rounded-xl bg-indigo-50 dark:bg-indigo-950/40 text-indigo-600 dark:text-indigo-400 flex items-center justify-center font-bold">
                  <TrendingUp className="w-5 h-5" />
                </div>
                <h3 className="font-bold text-neutral-900 dark:text-white text-base">
                  Transparent & Fair
                </h3>
                <p className="text-xs sm:text-sm text-neutral-600 dark:text-neutral-400 leading-relaxed">
                  No hidden account maintenance fees, zero unexpected deductions, and crystal-clear transaction receipts.
                </p>
              </div>
            </div>
          </div>
        </section>

        {/* Bottom CTA Banner */}
        <section className="py-20 bg-gradient-to-br from-blue-600 via-indigo-600 to-blue-700 text-white relative overflow-hidden">
          <div className="absolute top-0 right-0 w-96 h-96 bg-white/10 rounded-full blur-3xl pointer-events-none -mr-20 -mt-20" />
          <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 text-center space-y-6 relative z-10">
            <h2 className="text-3xl sm:text-5xl font-extrabold tracking-tight">
              Ready to take control of your financial journey?
            </h2>
            <p className="text-base sm:text-lg text-blue-100 max-w-2xl mx-auto leading-relaxed">
              Open your FinFlow digital account in less than two minutes and experience modern banking built around you.
            </p>
            <div className="pt-2 flex flex-col sm:flex-row items-center justify-center gap-3">
              <Link
                to="/register"
                className="w-full sm:w-auto px-8 py-3.5 bg-white text-blue-600 hover:bg-blue-50 font-bold rounded-xl text-base shadow-xl transition-all active:scale-95"
              >
                Open an Account
              </Link>
              <Link
                to="/login"
                className="w-full sm:w-auto px-8 py-3.5 bg-blue-700/60 hover:bg-blue-700 text-white border border-white/20 font-semibold rounded-xl text-base transition-all"
              >
                Sign In to FinFlow
              </Link>
            </div>
          </div>
        </section>
      </main>

      {/* Footer */}
      <footer className="bg-white dark:bg-[#080b11] border-t border-neutral-200/80 dark:border-neutral-800/80 py-12 text-xs text-neutral-500 dark:text-neutral-400">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex flex-col sm:flex-row items-center justify-between gap-6">
          <div className="flex items-center gap-2.5">
            <div className="w-7 h-7 rounded-lg bg-blue-600 flex items-center justify-center font-bold text-white text-xs">
              F
            </div>
            <span className="font-bold text-neutral-800 dark:text-neutral-200 text-sm">
              FinFlow
            </span>
            <span className="text-neutral-400">© 2026 FinFlow Technologies. All rights reserved.</span>
          </div>

          <div className="flex items-center gap-6">
            <a href="#features" className="hover:text-neutral-700 dark:hover:text-neutral-200 transition-colors">
              Features
            </a>
            <a href="#security" className="hover:text-neutral-700 dark:hover:text-neutral-200 transition-colors">
              Security
            </a>
            <Link to="/login" className="hover:text-neutral-700 dark:hover:text-neutral-200 transition-colors">
              Sign In
            </Link>
            <Link to="/register" className="hover:text-neutral-700 dark:hover:text-neutral-200 transition-colors">
              Open Account
            </Link>
          </div>
        </div>
      </footer>
    </div>
  );
};
