export function formatCurrency(amount: number | null | undefined, currency: string = 'INR'): string {
  if (amount === null || amount === undefined || isNaN(amount)) {
    return `₹0.00`;
  }

  const formattedNum = new Intl.NumberFormat('en-IN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(amount);

  if (currency === 'INR') {
    return `₹${formattedNum}`;
  }

  return `${formattedNum} ${currency}`;
}

export function formatDate(dateString: string | null | undefined): string {
  if (!dateString) return '';
  try {
    const date = new Date(dateString);
    return new Intl.DateTimeFormat('en-IN', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
    }).format(date);
  } catch {
    return dateString;
  }
}

export function formatShortDate(dateString: string | null | undefined): string {
  if (!dateString) return '';
  try {
    const date = new Date(dateString);
    return new Intl.DateTimeFormat('en-IN', {
      month: 'short',
      day: 'numeric',
    }).format(date);
  } catch {
    return dateString;
  }
}

export function formatFullDateTime(dateString: string | null | undefined): string {
  if (!dateString) return '';
  try {
    const date = new Date(dateString);
    return new Intl.DateTimeFormat('en-IN', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      hour12: true,
    }).format(date);
  } catch {
    return dateString;
  }
}

export function formatRelativeDate(dateString: string | null | undefined): string {
  if (!dateString) return '';
  try {
    const date = new Date(dateString);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffSec = Math.floor(diffMs / 1000);
    const diffMin = Math.floor(diffSec / 60);
    const diffHours = Math.floor(diffMin / 60);
    const diffDays = Math.floor(diffHours / 24);

    if (diffSec < 60) return 'Just now';
    if (diffMin < 60) return `${diffMin}m ago`;
    if (diffHours < 24) return `${diffHours}h ago`;
    if (diffDays < 7) return `${diffDays}d ago`;

    return formatDate(dateString);
  } catch {
    return dateString;
  }
}

export function formatMaskedAccount(accountNumber: string | null | undefined): string {
  if (!accountNumber) return '••••';
  const clean = accountNumber.trim();
  if (clean.length <= 4) return clean;
  return `••••${clean.slice(-4)}`;
}

export const maskAccountNumber = formatMaskedAccount;

export function getTransactionTypeLabel(type: string): string {
  switch (type) {
    case 'DEPOSIT':
      return 'Account Deposit';
    case 'WITHDRAW':
      return 'Account Withdrawal';
    case 'TRANSFER_SENT':
      return 'Outgoing Transfer';
    case 'TRANSFER_RECEIVED':
      return 'Incoming Transfer';
    case 'TRANSFER':
      return 'Account Transfer';
    default:
      return type;
  }
}
