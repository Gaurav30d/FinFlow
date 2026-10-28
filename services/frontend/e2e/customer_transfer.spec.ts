import { test, expect } from '@playwright/test';

test.describe('End-to-End Customer Transfer Flow', () => {
  test('sender registers & deposits, transfers money to recipient, and recipient verifies balance, transactions, and notifications', async ({ page }) => {
    test.setTimeout(90000);
    const uid = Math.random().toString(36).substring(2, 7) + Date.now().toString().slice(-4);

    const recipientUser = {
      name: `Recipient ${uid}`,
      username: `rec_${uid}`,
      email: `rec_${uid}@finflow.test`,
      password: `P@ssword_${uid}!`,
    };

    const senderUser = {
      name: `Sender ${uid}`,
      username: `send_${uid}`,
      email: `send_${uid}@finflow.test`,
      password: `P@ssword_${uid}!`,
    };

    const performSignOut = async () => {
      const mobileDrawerBtn = page.getByLabel('Toggle navigation drawer');
      if (await mobileDrawerBtn.isVisible()) {
        await mobileDrawerBtn.click();
        await page.waitForTimeout(300);
      }
      const signOutBtn = page.getByRole('button', { name: /Sign Out/i }).first();
      await signOutBtn.click();
      await expect(page).toHaveURL(/\/login/, { timeout: 10000 });
    };

    // ==========================================
    // STEP 1: Register Recipient and capture Account Number
    // ==========================================
    await page.goto('/register');
    await page.getByPlaceholder('e.g. Alex Morgan').fill(recipientUser.name);
    await page.getByPlaceholder('e.g. alex_m').fill(recipientUser.username);
    await page.getByPlaceholder('alex@example.com').fill(recipientUser.email);
    await page.getByPlaceholder('••••••••').first().fill(recipientUser.password);
    await page.getByPlaceholder('••••••••').nth(1).fill(recipientUser.password);
    await page.getByRole('button', { name: 'Open Account' }).click();

    await expect(page).toHaveURL(/\/dashboard/, { timeout: 15000 });
    await expect(page.getByText(/Available Balance/i).first()).toBeVisible({ timeout: 15000 });

    // Fetch recipient's account number via authenticated /api/accounts/me endpoint with retry
    const recipientAccountNumber = await page.evaluate(async () => {
      const token = localStorage.getItem('finflow_token');
      for (let attempt = 0; attempt < 5; attempt++) {
        try {
          let res = await fetch('/api/accounts/me', {
            headers: { Authorization: `Bearer ${token}` }
          });
          if (res.ok) {
            const data = await res.json();
            if (data?.accountNumber) return data.accountNumber;
          }
          // Fallback create account if Kafka consumer hasn't finished yet
          res = await fetch('/api/accounts', {
            method: 'POST',
            headers: { Authorization: `Bearer ${token}` }
          });
          if (res.ok) {
            const data = await res.json();
            if (data?.accountNumber) return data.accountNumber;
          }
        } catch {}
        await new Promise((r) => setTimeout(r, 600));
      }
      return null;
    });

    expect(recipientAccountNumber).toBeTruthy();
    expect(recipientAccountNumber!.length).toBeGreaterThanOrEqual(10);

    // Logout recipient
    await performSignOut();

    // ==========================================
    // STEP 2: Register Sender, Deposit Funds, and Send Money
    // ==========================================
    await page.goto('/register');
    await page.getByPlaceholder('e.g. Alex Morgan').fill(senderUser.name);
    await page.getByPlaceholder('e.g. alex_m').fill(senderUser.username);
    await page.getByPlaceholder('alex@example.com').fill(senderUser.email);
    await page.getByPlaceholder('••••••••').first().fill(senderUser.password);
    await page.getByPlaceholder('••••••••').nth(1).fill(senderUser.password);
    await page.getByRole('button', { name: 'Open Account' }).click();

    await expect(page).toHaveURL(/\/dashboard/, { timeout: 15000 });
    await expect(page.getByText(/Available Balance/i).first()).toBeVisible({ timeout: 15000 });

    // Deposit ₹1,000 into Sender Account
    const addFundsBtn = page.getByRole('button', { name: /Add Funds/i }).first();
    await addFundsBtn.click();
    await expect(page.getByRole('heading', { name: 'Add Money' })).toBeVisible({ timeout: 10000 });

    const confirmDepositBtn = page.getByRole('button', { name: 'Confirm Deposit' });
    await expect(confirmDepositBtn).toBeVisible();
    await confirmDepositBtn.click();

    const depositDoneBtn = page.getByRole('button', { name: 'Done' });
    await expect(depositDoneBtn).toBeVisible({ timeout: 15000 });
    await depositDoneBtn.click();
    await expect(page.getByRole('heading', { name: 'Add Money' })).toBeHidden();

    // Verify Sender balance reflects ₹1,000.00
    await expect(page.getByText(/₹1,000\.00/).first()).toBeVisible({ timeout: 10000 });

    // Navigate to Send Money page (/dashboard/transfers)
    await page.getByRole('link', { name: /Send Money/i }).first().click();
    await expect(page).toHaveURL(/\/dashboard\/transfers/, { timeout: 10000 });
    await expect(page.getByRole('heading', { name: 'Send Money' })).toBeVisible();

    // Fill Transfer Details
    await page.getByPlaceholder('Enter recipient account number').fill(recipientAccountNumber);
    await page.getByPlaceholder('0.00').fill('350.00');
    await page.getByPlaceholder(/Dinner share, Rent, Gift/i).fill('QA E2E Settlement');

    // Review transfer
    await page.getByRole('button', { name: /Review Transfer Details/i }).click();
    await expect(page.getByText(/You are sending/i)).toBeVisible({ timeout: 10000 });

    // Confirm & Send
    const confirmSendBtn = page.getByRole('button', { name: /Confirm & Send/i });
    await expect(confirmSendBtn).toBeVisible();
    await confirmSendBtn.click();

    // Verify Success Screen
    await expect(page.getByText(/Transfer Successful!/i)).toBeVisible({ timeout: 15000 });
    await expect(page.getByText(/Confirmed & Completed/i)).toBeVisible();

    // Navigate back to Dashboard and verify deducted balance
    await page.getByRole('link', { name: 'Dashboard' }).first().click();
    await expect(page).toHaveURL(/\/dashboard/);
    await expect(page.getByText(/₹650\.00/).first()).toBeVisible({ timeout: 10000 });

    // Logout Sender
    await performSignOut();

    // ==========================================
    // STEP 3: Log in as Recipient and verify received balance, transactions, and notifications
    // ==========================================
    await page.getByPlaceholder('yourname@example.com').fill(recipientUser.email);
    await page.getByPlaceholder('••••••••••••').fill(recipientUser.password);
    await page.getByRole('button', { name: 'Sign In' }).click();

    await expect(page).toHaveURL(/\/dashboard/, { timeout: 15000 });
    await expect(page.getByText(/Available Balance/i).first()).toBeVisible({ timeout: 15000 });

    // 1. Verify Recipient's received balance reflects ₹350.00
    await expect(page.getByText(/₹350\.00/).first()).toBeVisible({ timeout: 10000 });

    // 2. Navigate to Transactions Page and confirm transaction history
    await page.getByRole('link', { name: /Transactions/i }).first().click();
    await expect(page).toHaveURL(/\/dashboard\/transactions/);
    await expect(page.getByRole('heading', { name: 'Transactions' })).toBeVisible();
    await expect(page.getByText(/₹350\.00/).first()).toBeVisible({ timeout: 10000 });

    // 3. Navigate to Notifications Page and verify notification
    await page.getByRole('link', { name: /Notifications/i }).first().click();
    await expect(page).toHaveURL(/\/dashboard\/notifications/);
    await expect(page.getByRole('heading', { name: /Notifications & Security Feed/i })).toBeVisible();

    // Wait for Kafka notification consumer to deliver notification
    await expect(page.getByText(/350\.00|received|Transfer Received/i).first()).toBeVisible({ timeout: 15000 });
  });
});
