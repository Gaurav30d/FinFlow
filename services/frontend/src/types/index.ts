export type TransactionType = 'DEPOSIT' | 'WITHDRAW' | 'TRANSFER' | 'TRANSFER_SENT' | 'TRANSFER_RECEIVED';
export type TransactionStatus = 'COMPLETED' | 'PENDING' | 'FAILED';

export interface User {
  id: number;
  email: string;
  username?: string;
  name?: string;
  role?: string;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken?: string;
  tokenType?: string;
}

export interface Account {
  id: number;
  accountNumber: string;
  userId: number;
  balance: number;
  currency: string;
  status: string;
  createdAt: string;
}

export interface Transaction {
  id: number;
  transactionReference: string;
  type: TransactionType;
  amount: number;
  currency: string;
  status: TransactionStatus;
  senderAccountNumber?: string | null;
  receiverAccountNumber?: string | null;
  senderUserId?: number | null;
  receiverUserId?: number | null;
  description?: string;
  createdAt: string;
}

export interface NotificationItem {
  id: string | number;
  userId?: number;
  title?: string;
  message: string;
  type?: string;
  status?: string;
  read?: boolean;
  isRead?: boolean;
  createdAt: string;
}

export interface FinancialSummary {
  totalInflow: number;
  totalOutflow: number;
  netCashFlow: number;
  totalDeposits: number;
  totalWithdrawals: number;
  totalTransfersSent: number;
  totalTransfersReceived: number;
  completedTransactionCount: number;
  currentBalance: number;
  currency: string;
  hasTransactions: boolean;
  recentTransactions?: Array<{
    transactionReference: string;
    type: string;
    amount: number;
    currency: string;
    description: string;
    createdAt: string;
  }>;
}

export interface AiInsightsResponse {
  summary: FinancialSummary;
  insights: string;
  disclaimer?: string;
}

export interface AiChatResponse {
  question: string;
  answer: string;
  groundedSummary: FinancialSummary;
  disclaimer?: string;
}
