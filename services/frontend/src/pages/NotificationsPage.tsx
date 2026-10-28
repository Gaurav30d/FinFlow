import React, { useState, useEffect, useCallback } from 'react';
import {
  Bell,
  CheckCheck,
  AlertCircle,
  RefreshCw
} from 'lucide-react';
import { notificationsApi } from '../api/client';
import type { NotificationItem } from '../types';
import { formatFullDateTime } from '../utils/formatters';
import { useToast } from '../context/ToastContext';

export const NotificationsPage: React.FC = () => {
  const { showToast } = useToast();
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  const loadNotifications = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await notificationsApi.getNotifications();
      setNotifications(data);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Unable to load notifications';
      setError(msg);
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    loadNotifications();
  }, [loadNotifications]);

  const handleMarkAsRead = async (id: string | number) => {
    try {
      await notificationsApi.markAsRead(id);
      setNotifications((prev) =>
        prev.map((n) => (String(n.id) === String(id) ? { ...n, isRead: true, read: true } : n))
      );
      showToast('Notification marked as read', 'success');
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to update notification';
      showToast(msg, 'error');
    }
  };

  const isItemRead = (n: NotificationItem) => Boolean(n.isRead || n.read);
  const unreadCount = notifications.filter((n) => !isItemRead(n)).length;

  return (
    <div className="space-y-6 animate-fade-in max-w-4xl mx-auto">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight text-neutral-900 dark:text-white">
              Notifications & Security Feed
            </h1>
            {unreadCount > 0 && (
              <span className="text-xs font-bold px-2.5 py-0.5 rounded-full bg-blue-100 dark:bg-blue-950 text-blue-700 dark:text-blue-300 border border-blue-200 dark:border-blue-800">
                {unreadCount} unread
              </span>
            )}
          </div>
          <p className="text-xs sm:text-sm text-neutral-500 dark:text-neutral-400 mt-1">
            Real-time notifications for balance updates, incoming transfers, and security events
          </p>
        </div>

        <button
          onClick={loadNotifications}
          disabled={isLoading}
          className="self-start sm:self-auto p-2 rounded-xl bg-white dark:bg-neutral-800 border border-neutral-200 dark:border-neutral-700 text-neutral-700 dark:text-neutral-200 hover:bg-neutral-50 dark:hover:bg-neutral-700 transition-all disabled:opacity-50"
        >
          <RefreshCw className={`w-4 h-4 ${isLoading ? 'animate-spin' : ''}`} />
        </button>
      </div>

      {/* Notifications list */}
      <div className="rounded-2xl bg-white dark:bg-[#121620] border border-neutral-200/80 dark:border-neutral-800/80 shadow-sm overflow-hidden">
        {isLoading ? (
          <div className="p-6 space-y-3">
            {[1, 2, 3, 4].map((i) => (
              <div
                key={i}
                className="p-4 rounded-xl bg-neutral-50/60 dark:bg-neutral-900/40 animate-pulse space-y-2"
              >
                <div className="h-4 w-48 bg-neutral-200 dark:bg-neutral-800 rounded" />
                <div className="h-3 w-72 bg-neutral-200 dark:bg-neutral-800 rounded" />
              </div>
            ))}
          </div>
        ) : error ? (
          <div className="p-12 text-center">
            <div className="w-12 h-12 rounded-xl bg-rose-50 dark:bg-rose-950/40 text-rose-600 dark:text-rose-400 mx-auto flex items-center justify-center mb-3">
              <AlertCircle className="w-6 h-6" />
            </div>
            <h3 className="font-semibold text-neutral-900 dark:text-white">
              Failed to load notifications
            </h3>
            <p className="text-xs text-neutral-500 dark:text-neutral-400 mt-1 max-w-sm mx-auto">
              {error}
            </p>
            <button
              onClick={loadNotifications}
              className="mt-4 px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white text-xs font-semibold rounded-xl"
            >
              Try Again
            </button>
          </div>
        ) : notifications.length === 0 ? (
          <div className="p-12 text-center">
            <div className="w-12 h-12 rounded-xl bg-neutral-100 dark:bg-neutral-800 text-neutral-400 mx-auto flex items-center justify-center mb-3">
              <Bell className="w-6 h-6" />
            </div>
            <h3 className="font-semibold text-neutral-900 dark:text-white">
              All caught up!
            </h3>
            <p className="text-xs text-neutral-500 dark:text-neutral-400 mt-1 max-w-sm mx-auto">
              You do not have any pending alerts or security notifications.
            </p>
          </div>
        ) : (
          <div className="divide-y divide-neutral-100 dark:divide-neutral-800/60">
            {notifications.map((n) => {
              const read = isItemRead(n);
              return (
                <div
                  key={n.id}
                  className={`p-4 sm:p-5 flex items-start justify-between gap-4 transition-colors ${
                    !read
                      ? 'bg-blue-50/30 dark:bg-blue-950/15'
                      : 'hover:bg-neutral-50/60 dark:hover:bg-neutral-800/30'
                  }`}
                >
                  <div className="flex items-start gap-3.5">
                    <div
                      className={`w-9 h-9 rounded-xl flex items-center justify-center shrink-0 border ${
                        !read
                          ? 'bg-blue-100 dark:bg-blue-900/40 text-blue-600 dark:text-blue-400 border-blue-200 dark:border-blue-800'
                          : 'bg-neutral-100 dark:bg-neutral-800 text-neutral-400 border-neutral-200 dark:border-neutral-700'
                      }`}
                    >
                      <Bell className="w-4 h-4" />
                    </div>

                    <div className="space-y-1">
                      <div className="flex items-center gap-2">
                        <h4 className="text-xs sm:text-sm font-semibold text-neutral-900 dark:text-white">
                          {n.title || 'Account Update'}
                        </h4>
                        {!read && (
                          <span className="w-2 h-2 rounded-full bg-blue-600" />
                        )}
                      </div>
                      <p className="text-xs text-neutral-600 dark:text-neutral-300 leading-relaxed">
                        {n.message}
                      </p>
                      <span className="text-[11px] text-neutral-400 block pt-0.5">
                        {formatFullDateTime(n.createdAt)}
                      </span>
                    </div>
                  </div>

                  {!read && (
                    <button
                      onClick={() => handleMarkAsRead(n.id)}
                      className="shrink-0 p-2 text-neutral-400 hover:text-blue-600 dark:hover:text-blue-400 text-xs flex items-center gap-1 font-medium transition-colors"
                      title="Mark as read"
                    >
                      <CheckCheck className="w-4 h-4" />
                      <span className="hidden sm:inline">Mark read</span>
                    </button>
                  )}
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
};
