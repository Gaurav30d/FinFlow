import React, { useState } from 'react';
import { Outlet } from 'react-router-dom';
import { Sidebar } from './Sidebar';
import { MobileNav } from './MobileNav';
import { TransferModal } from './TransferModal';
import { DepositModal } from './DepositModal';
import { useAuth } from '../context/AuthContext';

export const AppLayout: React.FC = () => {
  const { refreshAccount } = useAuth();
  const [isTransferModalOpen, setIsTransferModalOpen] = useState(false);
  const [isDepositModalOpen, setIsDepositModalOpen] = useState(false);

  return (
    <div className="min-h-screen bg-neutral-50 dark:bg-[#0c0e14] text-neutral-900 dark:text-neutral-100 flex flex-col md:flex-row antialiased">
      {/* Desktop Sidebar */}
      <Sidebar
        onOpenTransferModal={() => setIsTransferModalOpen(true)}
      />

      {/* Mobile Header, Slideover Drawer, and Bottom Bar */}
      <MobileNav
        onOpenTransferModal={() => setIsTransferModalOpen(true)}
      />

      {/* Main Content Area */}
      <main className="flex-1 min-w-0 flex flex-col pb-20 md:pb-8">
        <div className="flex-1 w-full max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pt-4 sm:pt-6">
          <Outlet
            context={{
              onOpenTransfer: () => setIsTransferModalOpen(true),
              onOpenDeposit: () => setIsDepositModalOpen(true),
            }}
          />
        </div>
      </main>

      {/* Global Modals */}
      <TransferModal
        isOpen={isTransferModalOpen}
        onClose={() => setIsTransferModalOpen(false)}
        onSuccess={() => {
          refreshAccount();
        }}
      />

      <DepositModal
        isOpen={isDepositModalOpen}
        onClose={() => setIsDepositModalOpen(false)}
        onSuccess={() => {
          refreshAccount();
        }}
      />
    </div>
  );
};
