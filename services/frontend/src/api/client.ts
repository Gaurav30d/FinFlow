import type {
  Account,
  AiChatResponse,
  AiInsightsResponse,
  LoginResponse,
  NotificationItem,
  Transaction,
  User,
} from '../types';

const BASE_URL = import.meta.env.VITE_API_URL || '/api';

export function generateIdempotencyKey(): string {
  if (typeof crypto !== 'undefined' && crypto.randomUUID) {
    return crypto.randomUUID();
  }
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
    const r = (Math.random() * 16) | 0;
    const v = c === 'x' ? r : (r & 0x3) | 0x8;
    return v.toString(16);
  });
}

class ApiClient {
  private getToken(): string | null {
    return localStorage.getItem('finflow_token');
  }

  private async request<T>(
    endpoint: string,
    options: RequestInit = {}
  ): Promise<T> {
    const token = this.getToken();
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      Accept: 'application/json',
      ...((options.headers as Record<string, string>) || {}),
    };

    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }

    const url = endpoint.startsWith('http') ? endpoint : `${BASE_URL}${endpoint}`;

    const response = await fetch(url, {
      ...options,
      headers,
    });

    if (response.status === 401) {
      if (!endpoint.includes('/auth/login') && !endpoint.includes('/auth/register')) {
        localStorage.removeItem('finflow_token');
        localStorage.removeItem('finflow_user');
        window.dispatchEvent(new Event('auth:unauthorized'));
      }
    }

    if (!response.ok) {
      let errorMessage = `Request failed with status ${response.status}`;
      try {
        const text = await response.text();
        if (text) {
          try {
            const errorData = JSON.parse(text);
            errorMessage = errorData.message || errorData.error || text;
          } catch {
            errorMessage = text;
          }
        }
      } catch {
        // stream already consumed or empty
      }
      throw new Error(errorMessage);
    }

    if (response.status === 204) {
      return {} as T;
    }

    return response.json();
  }

  // --- Authentication ---
  auth = {
    login: async (usernameOrEmail: string, password: string): Promise<LoginResponse> => {
      // Backend may accept username or email
      return this.request<LoginResponse>('/auth/login', {
        method: 'POST',
        body: JSON.stringify({
          username: usernameOrEmail,
          email: usernameOrEmail,
          password,
        }),
      });
    },

    register: async (
      data: { name?: string; username?: string; email: string; password: string } | string,
      passwordArg?: string
    ): Promise<User> => {
      let payload;
      if (typeof data === 'string') {
        payload = { email: data, password: passwordArg };
      } else {
        payload = data;
      }
      return this.request<User>('/auth/register', {
        method: 'POST',
        body: JSON.stringify(payload),
      });
    },
  };

  // --- Accounts ---
  accounts = {
    getMyAccount: async (): Promise<Account> => {
      return this.request<Account>('/accounts/me');
    },

    createAccount: async (): Promise<Account> => {
      return this.request<Account>('/accounts', {
        method: 'POST',
      });
    },

    deposit: async (
      amountOrReq: number | { amount: number },
      idempotencyKey?: string
    ): Promise<Account> => {
      const amount = typeof amountOrReq === 'number' ? amountOrReq : amountOrReq.amount;
      const key = idempotencyKey || generateIdempotencyKey();
      return this.request<Account>('/accounts/me/deposit', {
        method: 'POST',
        headers: {
          'Idempotency-Key': key,
        },
        body: JSON.stringify({ amount }),
      });
    },

    withdraw: async (
      amountOrReq: number | { amount: number },
      idempotencyKey?: string
    ): Promise<Account> => {
      const amount = typeof amountOrReq === 'number' ? amountOrReq : amountOrReq.amount;
      const key = idempotencyKey || generateIdempotencyKey();
      return this.request<Account>('/accounts/me/withdraw', {
        method: 'POST',
        headers: {
          'Idempotency-Key': key,
        },
        body: JSON.stringify({ amount }),
      });
    },
  };

  // --- Transactions ---
  transactions = {
    getMyTransactions: async (): Promise<Transaction[]> => {
      return this.request<Transaction[]>('/transactions/me');
    },

    create: async (
      type: 'DEPOSIT' | 'WITHDRAW',
      amount: number,
      currency: string = 'INR',
      idempotencyKey?: string
    ): Promise<Transaction> => {
      const key = idempotencyKey || generateIdempotencyKey();
      return this.request<Transaction>('/transactions', {
        method: 'POST',
        headers: {
          'Idempotency-Key': key,
        },
        body: JSON.stringify({ type, amount, currency }),
      });
    },

    transfer: async (
      reqOrReceiver:
        | {
            toAccountNumber?: string;
            receiverAccountNumber?: string;
            amount: number;
            currency?: string;
            description?: string;
          }
        | string,
      amountArg?: number,
      currencyArg: string = 'INR',
      idempotencyKeyArg?: string
    ): Promise<Transaction> => {
      let receiver: string;
      let amount: number;
      let currency = 'INR';
      let description: string | undefined;
      let key = idempotencyKeyArg;

      if (typeof reqOrReceiver === 'string') {
        receiver = reqOrReceiver;
        amount = amountArg || 0;
        currency = currencyArg || 'INR';
      } else {
        receiver = reqOrReceiver.toAccountNumber || reqOrReceiver.receiverAccountNumber || '';
        amount = reqOrReceiver.amount;
        currency = reqOrReceiver.currency || 'INR';
        description = reqOrReceiver.description;
        if (amountArg && typeof amountArg === 'string') {
          key = amountArg;
        }
      }

      const effectiveKey = key || generateIdempotencyKey();

      return this.request<Transaction>('/transactions/transfer', {
        method: 'POST',
        headers: {
          'Idempotency-Key': effectiveKey,
        },
        body: JSON.stringify({
          receiverAccountNumber: receiver,
          amount,
          currency,
          description,
        }),
      });
    },
  };

  // --- AI Financial Assistant ---
  ai = {
    getInsights: async (): Promise<AiInsightsResponse> => {
      return this.request<AiInsightsResponse>('/ai/insights');
    },

    chat: async (question: string): Promise<AiChatResponse> => {
      return this.request<AiChatResponse>('/ai/chat', {
        method: 'POST',
        body: JSON.stringify({ question }),
      });
    },

    askQuestion: async (req: { question: string }): Promise<AiChatResponse> => {
      return this.request<AiChatResponse>('/ai/chat', {
        method: 'POST',
        body: JSON.stringify(req),
      });
    },
  };

  // --- Notifications ---
  notifications = {
    getNotifications: async (): Promise<NotificationItem[]> => {
      return this.request<NotificationItem[]>('/notifications');
    },

    getAll: async (): Promise<NotificationItem[]> => {
      return this.request<NotificationItem[]>('/notifications');
    },

    getUnreadCount: async (): Promise<number> => {
      return this.request<number>('/notifications/unread-count');
    },

    markAsRead: async (id: string | number): Promise<void> => {
      return this.request<void>(`/notifications/${id}/read`, {
        method: 'PATCH',
      });
    },
  };
}

export const api = new ApiClient();

export const authApi = api.auth;
export const accountsApi = api.accounts;
export const transactionsApi = api.transactions;
export const aiApi = api.ai;
export const notificationsApi = api.notifications;
