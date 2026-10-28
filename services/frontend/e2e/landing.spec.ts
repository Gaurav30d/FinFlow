import { test, expect } from '@playwright/test';

test.describe('FinFlow Public Landing Page', () => {
  test('loads cleanly without errors and presents customer value proposition', async ({ page }) => {
    const consoleErrors: string[] = [];
    page.on('console', (msg) => {
      if (msg.type() === 'error') consoleErrors.push(msg.text());
    });

    await page.goto('/');

    // Check Headline and Supporting Text
    await expect(page.getByRole('heading', { level: 1 })).toContainText('Your money. Your goals.');
    await expect(page.getByText('Manage your money, make transfers, track your spending')).toBeVisible();

    // Check CTAs
    const openAccountButtons = page.getByRole('link', { name: 'Open an Account' });
    await expect(openAccountButtons.first()).toBeVisible();

    const signInButtons = page.getByRole('link', { name: 'Sign In' });
    await expect(signInButtons.first()).toBeVisible();

    // Check Illustrative Sample UI Preview
    await expect(page.getByText('Illustrative Sample UI')).toBeVisible();
    await expect(page.getByText('₹48,250.00')).toBeVisible();

    // Check Customer Benefits Section
    await expect(page.getByText('Customer-First Banking')).toBeVisible();
    await expect(page.getByText('Effortless Account Management')).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Instant Money Transfers' })).toBeVisible();
    await expect(page.getByText('Smart Spending Insights')).toBeVisible();

    // Check Security Section
    await expect(page.getByText('Security & Privacy First')).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Bank-Grade Encryption' })).toBeVisible();

    // Verify Theme Switching
    const html = page.locator('html');
    const themeBtn = page.getByTitle(/Switch to (Dark|Light) Mode/i).first();
    await expect(themeBtn).toBeVisible();

    const initialIsDark = await html.evaluate((el) => el.classList.contains('dark'));
    await themeBtn.click();
    await page.waitForTimeout(300);
    const afterClickIsDark = await html.evaluate((el) => el.classList.contains('dark'));
    expect(afterClickIsDark).not.toBe(initialIsDark);

    // Refresh and check persistence
    await page.reload();
    const afterReloadIsDark = await html.evaluate((el) => el.classList.contains('dark'));
    expect(afterReloadIsDark).toBe(afterClickIsDark);

    // Ensure no unexpected fatal console errors
    const fatalErrors = consoleErrors.filter(
      (err) => !err.includes('Failed to load resource') && !err.includes('status of 404')
    );
    expect(fatalErrors).toHaveLength(0);
  });

  test('navigation to login and register works properly', async ({ page }) => {
    await page.goto('/');

    // Click Sign In
    await page.getByRole('link', { name: 'Sign In' }).first().click();
    await expect(page).toHaveURL(/\/login/);
    await expect(page.getByRole('heading', { name: 'Sign in to your account' })).toBeVisible();

    // Click Open an Account from login
    await page.getByRole('link', { name: 'Open an Account' }).click();
    await expect(page).toHaveURL(/\/register/);
    await expect(page.getByRole('heading', { name: 'Open your FinFlow account' })).toBeVisible();
  });
});
