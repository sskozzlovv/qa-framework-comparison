import { expect, type APIRequestContext, type APIResponse } from '@playwright/test';
import { TEST_KEY, type TestUser } from './data';

/** Thin typed wrapper over an APIRequestContext (Playwright's HTTP client). */
export class ApiClient {
  private token?: string;

  constructor(private readonly request: APIRequestContext) {}

  async login(user: TestUser): Promise<string> {
    const response = await this.request.post('/api/login', { data: user });
    const { token } = await ok<{ token: string }>(response);
    this.token = token;
    return token;
  }

  async addToCart(productId: number): Promise<void> {
    if (!this.token) throw new Error('login() first');
    await ok(await this.request.post('/api/cart', {
      data: { productId },
      headers: { 'X-Auth-Token': this.token },
    }));
  }

  async seedOrders(user: TestUser, count: number): Promise<void> {
    await ok(await this.request.post('/api/test/seed-orders', {
      data: { username: user.username, count },
      headers: { 'X-Test-Key': TEST_KEY },
    }));
  }

  /** The "/cleanup" hook: removes the cart and all orders of the user. */
  async cleanup(user: TestUser): Promise<void> {
    await ok(await this.request.post('/api/test/cleanup', {
      data: { username: user.username },
      headers: { 'X-Test-Key': TEST_KEY },
    }));
  }

  async state(user: TestUser): Promise<{ cartItems: number; orders: number }> {
    return ok(await this.request.get('/api/test/state', {
      params: { username: user.username },
      headers: { 'X-Test-Key': TEST_KEY },
    }));
  }
}

async function ok<T = unknown>(response: APIResponse): Promise<T> {
  // toBeOK prints status, URL and body of the response on failure
  await expect(response).toBeOK();
  return response.json() as Promise<T>;
}
