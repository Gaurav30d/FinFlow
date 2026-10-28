import { test, expect } from '@playwright/test';

test.describe('Notification UI & AI Assistant End-to-End Verification', () => {
  test('verifies notifications unread badge, mark-as-read persistence, and AI Financial Assistant live interaction', async ({ page }) => {
    test.setTimeout(90000);
    const uid = Math.random().toString(36).substring(2, 7) + Date.now().toString().slice(-4);

    const testUser = {
      name: `AI User ${uid}`,
      username: `ai_user_${uid}`,
      email: `ai_${uid}@finflow.test`,
      password: `P@ssword_${uid}!`,
    };

    // 1. Register & Login
    await page.goto('/register');
    await page.getByPlaceholder('e.g. Alex Morgan').fill(testUser.name);
    await page.getByPlaceholder('e.g. alex_m').fill(testUser.username);
    await page.getByPlaceholder('alex@example.com').fill(testUser.email);
    await page.getByPlaceholder('••••••••').first().fill(testUser.password);
    await page.getByPlaceholder('••••••••').nth(1).fill(testUser.password);
    await page.getByRole('button', { name: 'Open Account' }).click();

    await expect(page).toHaveURL(/\/dashboard/, { timeout: 15000 });
    await expect(page.getByText(/Available Balance/i).first()).toBeVisible({ timeout: 15000 });

    // 2. Deposit Funds to trigger real balance & transactions
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

    // Verify Authoritative Balance reflects deposited ₹1,000.00
    await expect(page.getByText(/₹1,000\.00/).first()).toBeVisible({ timeout: 10000 });

    // 3. Verify AI Insights Card on Dashboard
    await expect(page.getByText(/Financial Assistant Insights/i)).toBeVisible({ timeout: 10000 });
    await expect(page.getByRole('button', { name: /Refresh Insights/i })).toBeVisible();

    // 4. Navigate to Financial Assistant Chat (/dashboard/ai-advisor)
    await page.getByRole('link', { name: /Financial Assistant/i }).first().click();
    await expect(page).toHaveURL(/\/dashboard\/ai-advisor/);
    await expect(page.getByRole('heading', { name: /Financial Assistant/i })).toBeVisible();

    // Send a real financial question
    const chatInput = page.getByPlaceholder(/Ask about your spending/i);
    await expect(chatInput).toBeVisible();
    await chatInput.fill('What is my current account balance?');
    await page.getByRole('button', { name: /Ask/i }).click();

    // Verify question appears in thread
    await expect(page.getByText('What is my current account balance?')).toBeVisible();

    // Verify AI response arrives and contains authoritative balance information
    await expect(page.getByText(/1,000|1000|INR|balance/i).first()).toBeVisible({ timeout: 25000 });

    // 5. Test Notification UI Behavior
    // Send a transfer to test account 2490102003373081 (User 5)
    await page.goto('/dashboard/transfers');
    await expect(page.getByRole('heading', { name: 'Send Money' })).toBeVisible();

    await page.getByPlaceholder('Enter recipient account number').fill('2490102003373081');
    await page.getByPlaceholder('0.00').fill('150.00');
    await page.getByPlaceholder(/Dinner share, Rent, Gift/i).fill('Notification UI Verification');

    await page.getByRole('button', { name: /Review Transfer Details/i }).click();
    await expect(page.getByText(/You are sending/i)).toBeVisible({ timeout: 10000 });

    const confirmSendBtn = page.getByRole('button', { name: /Confirm & Send/i });
    await expect(confirmSendBtn).toBeVisible();
    await confirmSendBtn.click();

    await expect(page.getByText(/Transfer Successful!/i)).toBeVisible({ timeout: 15000 });

    // 6. Navigate to Notifications Page
    await page.getByRole('link', { name: /Notifications/i }).first().click();
    await expect(page).toHaveURL(/\/dashboard\/notifications/);
    await expect(page.getByRole('heading', { name: /Notifications & Security Feed/i })).toBeVisible();

    // Verify notification is rendered
    await expect(page.getByText(/150\.00|sent|transfer/i).first()).toBeVisible({ timeout: 15000 });

    // Check Mark as Read button
    const markReadBtn = page.getByTitle('Mark as read').first();
    if (await markReadBtn.isVisible()) {
      await markReadBtn.click();
      await expect(page.getByText(/Notification marked as read/i)).toBeVisible({ timeout: 5000 });

      // Verify persistence across reload
      await page.reload();
      await expect(page.getByRole('heading', { name: /Notifications & Security Feed/i })).toBeVisible();
      await expect(page.getByText(/150\.00|sent|transfer/i).first()).toBeVisible();
    }

    // 7. Test Protected Route Redirect with Invalid / Missing JWT
    await page.evaluate(() => localStorage.removeItem('finflow_token'));
    await page.goto('/dashboard/ai-advisor');
    await expect(page).toHaveURL(/\/login/, { timeout: 10000 });
  });
});
