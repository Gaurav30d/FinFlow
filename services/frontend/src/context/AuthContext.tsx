import React, { createContext, useContext, useEffect, useState, useCallback } from 'react';
import { api } from '../api/client';
import type { Account, User } from '../types';

interface AuthContextType {
  token: string | null;
  user: User | null;
  account: Account | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, password: string) => Promise<void>;
  logout: () => void;
  refreshAccount: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

function parseJwt(token: string): { sub?: string; userId?: number; exp?: number } | null {
  try {
    const base64Url = token.split('.')[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(
      window
        .atob(base64)
        .split('')
        .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    );
    return JSON.parse(jsonPayload);
  } catch {
    return null;
  }
}

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [token, setToken] = useState<string | null>(() => localStorage.getItem('finflow_token'));
  const [user, setUser] = useState<User | null>(() => {
    const saved = localStorage.getItem('finflow_user');
    return saved ? JSON.parse(saved) : null;
  });
  const [account, setAccount] = useState<Account | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);

  const logout = useCallback(() => {
    localStorage.removeItem('finflow_token');
    localStorage.removeItem('finflow_user');
    setToken(null);
    setUser(null);
    setAccount(null);
  }, []);

  const refreshAccount = useCallback(async () => {
    if (!token) return;
    try {
      const acc = await api.accounts.getMyAccount();
      setAccount(acc);
    } catch {
      // If account does not exist yet for this user, attempt creating it
      try {
        const created = await api.accounts.createAccount();
        setAccount(created);
      } catch (e) {
        console.warn('Could not load or create account:', e);
      }
    }
  }, [token]);

  useEffect(() => {
    const handleUnauthorized = () => {
      logout();
    };
    window.addEventListener('auth:unauthorized', handleUnauthorized);
    return () => window.removeEventListener('auth:unauthorized', handleUnauthorized);
  }, [logout]);

  useEffect(() => {
    const initAuth = async () => {
      if (token) {
        const payload = parseJwt(token);
        if (payload && payload.exp && payload.exp * 1000 < Date.now()) {
          logout();
          setIsLoading(false);
          return;
        }

        if (payload && !user) {
          const derivedUser: User = {
            id: payload.userId || 0,
            email: payload.sub || 'user@finflow.com',
          };
          setUser(derivedUser);
          localStorage.setItem('finflow_user', JSON.stringify(derivedUser));
        }

        await refreshAccount();
      }
      setIsLoading(false);
    };

    initAuth();
  }, [token, refreshAccount, logout, user]);

  const login = async (email: string, password: string) => {
    const response = await api.auth.login(email, password);
    const jwtToken = response.accessToken;
    localStorage.setItem('finflow_token', jwtToken);
    setToken(jwtToken);

    const payload = parseJwt(jwtToken);
    const loggedUser: User = {
      id: payload?.userId || 0,
      email: payload?.sub || email,
    };
    localStorage.setItem('finflow_user', JSON.stringify(loggedUser));
    setUser(loggedUser);

    try {
      const acc = await api.accounts.getMyAccount();
      setAccount(acc);
    } catch {
      try {
        const created = await api.accounts.createAccount();
        setAccount(created);
      } catch (e) {
        console.warn('Account setup error:', e);
      }
    }
  };

  const register = async (email: string, password: string) => {
    await api.auth.register(email, password);
    // Automatically log in after registration
    await login(email, password);
  };

  return (
    <AuthContext.Provider
      value={{
        token,
        user,
        account,
        isAuthenticated: !!token,
        isLoading,
        login,
        register,
        logout,
        refreshAccount,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = (): AuthContextType => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
