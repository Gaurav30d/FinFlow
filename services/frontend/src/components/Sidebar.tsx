import React, { useEffect, useState } from 'react';
import { NavLink, useNavigate, Link } from 'react-router-dom';
import {
  LayoutDashboard,
  ArrowLeftRight,
  Send,
  Sparkles,
  Bell,
  LogOut,
  ChevronRight
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { ThemeToggle } from './ThemeToggle';
import { notificationsApi } from '../api/client';

interface SidebarProps {
  onOpenTransferModal: () => void;
}

export const Sidebar: React.FC<SidebarProps> = ({ onOpenTransferModal }) => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [unreadCount, setUnreadCount] = useState<number>(0);

  useEffect(() => {
    let isMounted = true;
    const fetchUnread = async () => {
      try {
        const count = await notificationsApi.getUnreadCount();
        if (isMounted) setUnreadCount(count);
      } catch {
        // silent fail for badge
      }
    };

    fetchUnread();
    const interval = setInterval(fetchUnread, 15000);
    return () => {
      isMounted = false;
      clearInterval(interval);
    };
  }, []);

  const navItems = [
    { name: 'Dashboard', path: '/dashboard', icon: LayoutDashboard },
    { name: 'Transactions', path: '/dashboard/transactions', icon: ArrowLeftRight },
    { name: 'Send Money', path: '/dashboard/transfers', icon: Send },
    {
      name: 'Financial Assistant',
      path: '/dashboard/ai-advisor',
      icon: Sparkles,
      badge: 'Smart',
      badgeClass: 'bg-blue-100 dark:bg-blue-950/80 text-blue-700 dark:text-blue-300 border border-blue-200 dark:border-blue-800',
    },
    {
      name: 'Notifications',
      path: '/dashboard/notifications',
      icon: Bell,
      badge: unreadCount > 0 ? String(unreadCount) : undefined,
      badgeClass: 'bg-blue-600 text-white font-bold',
    },
  ];

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <aside className="hidden md:flex flex-col w-64 border-r border-neutral-200/80 dark:border-neutral-800/80 bg-white dark:bg-[#0e121b] shrink-0 h-screen sticky top-0 z-30 select-none">
      {/* Brand Header */}
      <div className="h-16 px-6 flex items-center gap-3 border-b border-neutral-100 dark:border-neutral-800/80">
        <Link to="/dashboard" className="flex items-center gap-2.5">
          <div className="w-8 h-8 rounded-xl bg-gradient-to-tr from-blue-600 to-indigo-600 flex items-center justify-center font-bold text-white shadow-md shadow-blue-500/20 text-base">
            F
          </div>
          <div>
            <span className="font-extrabold text-base tracking-tight text-neutral-900 dark:text-white">
              FinFlow
            </span>
            <span className="block text-[10px] text-neutral-400 dark:text-neutral-500 font-medium leading-none">
              Digital Banking
            </span>
          </div>
        </Link>
      </div>

      {/* Quick Action Button */}
      <div className="p-4 pb-2">
        <button
          onClick={onOpenTransferModal}
          type="button"
          className="w-full py-2.5 px-4 rounded-xl bg-blue-600 hover:bg-blue-500 text-white font-medium text-sm shadow-md shadow-blue-600/20 active:scale-[0.98] hover-lift transition-all flex items-center justify-center gap-2 group"
        >
          <Send className="w-4 h-4 transition-transform group-hover:translate-x-0.5" />
          <span>Send Money</span>
        </button>
      </div>

      {/* Navigation Links */}
      <nav className="flex-1 px-3 py-3 space-y-1 overflow-y-auto">
        {navItems.map((item) => (
          <NavLink
            key={item.path}
            to={item.path}
            end={item.path === '/dashboard'}
            className={({ isActive }) =>
              `flex items-center justify-between px-3.5 py-2.5 rounded-xl text-xs sm:text-sm font-medium transition-all group ${
                isActive
                  ? 'bg-blue-50 dark:bg-blue-950/50 text-blue-600 dark:text-blue-400 font-semibold shadow-xs'
                  : 'text-neutral-600 dark:text-neutral-400 hover:text-neutral-900 dark:hover:text-white hover:bg-neutral-100/70 dark:hover:bg-neutral-800/50'
              }`
            }
          >
            {({ isActive }) => (
              <>
                <div className="flex items-center gap-3">
                  <item.icon
                    className={`w-4 h-4 transition-colors ${
                      isActive
                        ? 'text-blue-600 dark:text-blue-400'
                        : 'text-neutral-400 group-hover:text-neutral-600 dark:group-hover:text-neutral-300'
                    }`}
                  />
                  <span>{item.name}</span>
                </div>

                {item.badge && (
                  <span
                    className={`text-[10px] font-bold px-2 py-0.5 rounded-full uppercase tracking-wider ${item.badgeClass}`}
                  >
                    {item.badge}
                  </span>
                )}
              </>
            )}
          </NavLink>
        ))}
      </nav>

      {/* User Footer Profile & Settings */}
      <div className="p-3 border-t border-neutral-100 dark:border-neutral-800/80 space-y-2">
        <div className="flex items-center justify-between p-2 rounded-xl bg-neutral-50 dark:bg-neutral-900/60 border border-neutral-200/50 dark:border-neutral-800/50">
          <div className="flex items-center gap-2.5 min-w-0">
            <div className="w-8 h-8 rounded-full bg-gradient-to-tr from-blue-600 to-indigo-600 text-white font-bold text-xs flex items-center justify-center shrink-0">
              {user?.email?.charAt(0).toUpperCase() || 'U'}
            </div>
            <div className="min-w-0">
              <div className="text-xs font-semibold text-neutral-800 dark:text-neutral-200 truncate">
                {user?.name || user?.username || user?.email?.split('@')[0] || 'Customer'}
              </div>
              <div className="text-[11px] text-neutral-400 truncate">
                {user?.email || 'Member'}
              </div>
            </div>
          </div>
          <ThemeToggle />
        </div>

        <button
          onClick={handleLogout}
          type="button"
          className="w-full flex items-center justify-between px-3 py-2 rounded-lg text-xs font-medium text-neutral-500 hover:text-rose-600 dark:hover:text-rose-400 hover:bg-rose-50 dark:hover:bg-rose-950/30 transition-colors"
        >
          <div className="flex items-center gap-2">
            <LogOut className="w-3.5 h-3.5" />
            <span>Sign Out</span>
          </div>
          <ChevronRight className="w-3 h-3 opacity-60" />
        </button>
      </div>
    </aside>
  );
};
