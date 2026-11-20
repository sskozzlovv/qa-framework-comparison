import { randomUUID } from 'node:crypto';

export const TEST_KEY = 'let-me-in';
export const PASSWORD = 'secret';

export type UserKind = 'normal' | 'slow' | 'locked';

export interface TestUser {
  username: string;
  password: string;
}

const PREFIX: Record<UserKind, string> = { normal: 'user_', slow: 'slow_', locked: 'locked_' };

/** A new user per test = an isolated cart and order list -> parallel tests and cleanup never collide. */
export function uniqueUser(kind: UserKind = 'normal'): TestUser {
  return { username: PREFIX[kind] + randomUUID().slice(0, 8), password: PASSWORD };
}

export interface Product {
  id: number;
  name: string;
  price: string;
}

export const products = {
  backpack: { id: 1, name: 'Trail Backpack', price: '$29.99' },
  bikeLight: { id: 2, name: 'Clip Bike Light', price: '$9.99' },
  tShirt: { id: 3, name: 'Cotton T-Shirt', price: '$15.99' },
  campMug: { id: 8, name: 'Camp Mug', price: '$6.99' },
} satisfies Record<string, Product>;

export const CATALOG_SIZE = 12;
