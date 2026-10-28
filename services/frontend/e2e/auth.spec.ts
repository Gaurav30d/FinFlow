import { test, expect } from '@playwright/test';

test.describe('FinFlow Customer Authentication Flow', () => {
  const uniqueId = Date.now().toString().slice(-6);
  const testUser = {
    name: `QA Customer ${uniqueId}`,
    username: `qa_user_${uniqueId}`,
    email: `qa_customer_${uniqueId}@finflow.test`,
    password: `Password_${uniqueId}!`,
  };

  test('validates registration form inputs and rejects invalid fields', async ({ page }) => {
    await page.goto('/register');

    // Attempt submit with empty fields
    await page.getByRole('button', { name: 'Open Account' }).click();

    // Fill short password (< 8 chars)
    await page.getByPlaceholder('e.g. Alex Morgan').fill('Test User');
    await page.getByPlaceholder('e.g. alex_m').fill('test_user');
    await page.getByPlaceholder('alex@example.com').fill('test_user@example.com');
    await page.getByPlaceholder('••••••••').first().fill('short');
    await page.getByPlaceholder('••••••••').nth(1).fill('short');
    await page.getByRole('button', { name: 'Open Account' }).click();
    await expect(page.getByText('Password must be at least 8 characters long')).toBeVisible();

    // Fill mismatched passwords
    await page.getByPlaceholder('••••••••').first().fill('Password123!');
    await page.getByPlaceholder('••••••••').nth(1).fill('DifferentPassword123!');
    await page.getByRole('button', { name: 'Open Account' }).click();
    await expect(page.getByText('Passwords do not match')).toBeVisible();
  });

  test('registers new customer, signs in, and manages session lifecycle', async ({ page }) => {
    const uniqueId = Math.random().toString(36).substring(2, 7) + Date.now().toString().slice(-4);
    const testUser = {
      name: `QA Customer ${uniqueId}`,
      username: `qa_user_${uniqueId}`,
      email: `qa_customer_${uniqueId}@finflow.test`,
      password: `Password_${uniqueId}!`,
    };

    await page.goto('/register');

    // Fill valid unique details
    await page.getByPlaceholder('e.g. Alex Morgan').fill(testUser.name);
    await page.getByPlaceholder('e.g. alex_m').fill(testUser.username);
    await page.getByPlaceholder('alex@example.com').fill(testUser.email);
    await page.getByPlaceholder('••••••••').first().fill(testUser.password);
    await page.getByPlaceholder('••••••••').nth(1).fill(testUser.password);

    // Test password toggle if available
    const toggleButtons = page.getByRole('button', { name: /Show password/i });
    if (await toggleButtons.count() > 0) {
      await toggleButtons.first().click();
      await expect(page.getByPlaceholder('••••••••').first()).toHaveAttribute('type', 'text');
    }

    // Submit registration
    await page.getByRole('button', { name: 'Open Account' }).click();

    // Should auto-login and navigate to dashboard
    await expect(page).toHaveURL(/\/dashboard/, { timeout: 15000 });
    await expect(page.getByText(/Available Balance/i).first()).toBeVisible();
    await expect(page.getByText(/Account Active & Verified/i)).toBeVisible();

    // Verify Session Persistence across page reload
    await page.reload();
    await expect(page).toHaveURL(/\/dashboard/);
    await expect(page.getByText(/Available Balance/i).first()).toBeVisible();

    // Test Logout
    const mobileDrawerBtn = page.getByLabel('Toggle navigation drawer');
    if (await mobileDrawerBtn.isVisible()) {
      await mobileDrawerBtn.click();
      await page.waitForTimeout(300);
    }
    const signOutBtn = page.getByRole('button', { name: /Sign Out/i }).first();
    await signOutBtn.click();
    await expect(page).toHaveURL(/\/login/);

    // Test Protected Route Redirect after logout
    await page.goto('/dashboard');
    await expect(page).toHaveURL(/\/login/);

    // Test Invalid Login Credentials
    await page.getByPlaceholder('yourname@example.com').fill(testUser.email);
    await page.getByPlaceholder('••••••••••••').fill('WrongPassword999!');
    await page.getByRole('button', { name: 'Sign In' }).click();
    await expect(page.getByText(/Invalid/i).first()).toBeVisible({ timeout: 10000 });

    // Test Valid Login
    await page.getByPlaceholder('••••••••••••').fill(testUser.password);
    await page.getByRole('button', { name: 'Sign In' }).click();
    await expect(page).toHaveURL(/\/dashboard/, { timeout: 15000 });
  });
});
