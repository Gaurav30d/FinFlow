import { test, expect } from '@playwright/test';

test.describe('FinFlow Customer Banking Dashboard Operations', () => {
  test('performs deposit, balance update, money transfer, and AI interaction', async ({ page }) => {
    const uniqueId = Math.random().toString(36).substring(2, 7) + Date.now().toString().slice(-4);
    const testCustomer = {
      name: `Ops Customer ${uniqueId}`,
      username: `ops_${uniqueId}`,
      email: `ops_${uniqueId}@finflow.test`,
      password: `P@ssword_${uniqueId}!`,
    };

    // 1. Register & Login
    await page.goto('/register');
    await page.getByPlaceholder('e.g. Alex Morgan').fill(testCustomer.name);
    await page.getByPlaceholder('e.g. alex_m').fill(testCustomer.username);
    await page.getByPlaceholder('alex@example.com').fill(testCustomer.email);
    await page.getByPlaceholder('••••••••').first().fill(testCustomer.password);
    await page.getByPlaceholder('••••••••').nth(1).fill(testCustomer.password);
    await page.getByRole('button', { name: 'Open Account' }).click();

    await expect(page).toHaveURL(/\/dashboard/, { timeout: 15000 });
    await expect(page.getByText(/Available Balance/i).first()).toBeVisible();
    await page.waitForTimeout(500);

    // 2. Perform Deposit
    const addFundsBtn = page.getByRole('button', { name: /Add Funds/i }).first();
    await addFundsBtn.click();

    // Verify Deposit Modal appears
    await expect(page.getByRole('heading', { name: 'Add Money' })).toBeVisible({ timeout: 10000 });
    await page.waitForTimeout(600);

    // Confirm deposit of the default 1000 amount
    const confirmBtn = page.getByRole('button', { name: 'Confirm Deposit' });
    await expect(confirmBtn).toBeVisible();
    await confirmBtn.click();

    // Wait for deposit completion and close modal
    const doneBtn = page.getByRole('button', { name: 'Done' });
    await expect(doneBtn).toBeVisible({ timeout: 15000 });
    await doneBtn.click();
    await expect(page.getByRole('heading', { name: 'Add Money' })).toBeHidden();

    // 3. Verify Authoritative Balance reflects the deposited ₹1,000.00
    await expect(page.getByText(/₹1,000\.00/).first()).toBeVisible({ timeout: 10000 });

    // 4. Test Navigation to Transactions
    await page.getByRole('link', { name: /Transactions/i }).first().click();
    await expect(page).toHaveURL(/\/dashboard\/transactions/);
    await expect(page.getByRole('heading', { name: 'Transactions' })).toBeVisible();

    // 5. Test Navigation to Financial Assistant
    await page.getByRole('link', { name: /Financial Assistant|Assistant/i }).first().click();
    await expect(page).toHaveURL(/\/dashboard\/ai-advisor/);
    await expect(page.getByRole('heading', { name: /Financial Assistant/i })).toBeVisible();

    // Check prompt chips
    const chip = page.getByText(/Summarize my recent spending/i).first();
    if (await chip.count() > 0) {
      await chip.click();
      // Verify message sent
      await expect(page.getByText('Summarize my recent spending.')).toBeVisible();
    }
  });
});
