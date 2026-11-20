import { rmSync } from 'node:fs';
import { request, type FullConfig } from '@playwright/test';
import { TEST_KEY } from './support/data';

/** Runs once per `playwright test` after webServer is up - the equivalent of @BeforeSuite. */
export default async function globalSetup(config: FullConfig): Promise<void> {
  // Allure results are files kept between runs: old ones would show up in the report as retries
  rmSync('allure-results', { recursive: true, force: true });

  const api = await request.newContext({ baseURL: config.projects[0].use.baseURL });
  const response = await api.post('/api/test/reset', { headers: { 'X-Test-Key': TEST_KEY } });
  if (!response.ok()) {
    throw new Error(`Backend reset failed: ${response.status()} ${await response.text()}`);
  }
  await api.dispose();
}
