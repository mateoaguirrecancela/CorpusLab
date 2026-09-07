import { describe, expect, it } from 'vitest';
import { type TFunction } from 'i18next';
import {
  createForgotPasswordSchema,
  createLoginSchema,
  createResetPasswordSchema,
  createSignupSchema,
} from '@/modules/auth/schemas/authFormSchemas';

const t = ((key: string) => key) as TFunction;

function isoDateYearsAgo(years: number): string {
  const date = new Date();
  date.setFullYear(date.getFullYear() - years);
  return date.toISOString().slice(0, 10);
}

describe('createLoginSchema', () => {
  const schema = createLoginSchema(t);

  it('accepts a valid login', () => {
    expect(schema.safeParse({ email: 'a@b.com', password: 'password1' }).success).toBe(true);
  });

  it('rejects an invalid email', () => {
    expect(schema.safeParse({ email: 'not-an-email', password: 'password1' }).success).toBe(false);
  });

  it('rejects a short password', () => {
    expect(schema.safeParse({ email: 'a@b.com', password: 'short' }).success).toBe(false);
  });

  it('rejects unknown fields (strict schema)', () => {
    expect(schema.safeParse({ email: 'a@b.com', password: 'password1', extra: 'x' }).success).toBe(
      false,
    );
  });
});

describe('createSignupSchema', () => {
  const schema = createSignupSchema(t);

  const validPayload = {
    email: 'a@b.com',
    firstName: 'Jane',
    lastName: 'Doe',
    birth: isoDateYearsAgo(20),
    gender: 'FEMALE',
    countryCode: 'ES',
    city: 'Coruna',
    password: 'password1',
  };

  it('accepts a fully valid signup', () => {
    expect(schema.safeParse(validPayload).success).toBe(true);
  });

  it('rejects an underage birth date', () => {
    const result = schema.safeParse({ ...validPayload, birth: isoDateYearsAgo(10) });
    expect(result.success).toBe(false);
  });

  it('rejects an empty gender', () => {
    expect(schema.safeParse({ ...validPayload, gender: '' }).success).toBe(false);
  });

  it('rejects a country code that is not exactly 2 characters', () => {
    expect(schema.safeParse({ ...validPayload, countryCode: 'ESP' }).success).toBe(false);
  });
});

describe('createForgotPasswordSchema', () => {
  const schema = createForgotPasswordSchema(t);

  it('requires a valid email', () => {
    expect(schema.safeParse({ email: 'a@b.com' }).success).toBe(true);
    expect(schema.safeParse({ email: '' }).success).toBe(false);
  });
});

describe('createResetPasswordSchema', () => {
  const schema = createResetPasswordSchema(t);

  it('accepts matching passwords with a token', () => {
    expect(
      schema.safeParse({
        token: 'a'.repeat(16),
        newPassword: 'password1',
        confirmPassword: 'password1',
      }).success,
    ).toBe(true);
  });

  it('rejects mismatched passwords, attaching the error to confirmPassword', () => {
    const result = schema.safeParse({
      token: 'a'.repeat(16),
      newPassword: 'password1',
      confirmPassword: 'password2',
    });

    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues[0].path).toEqual(['confirmPassword']);
    }
  });

  it('rejects a token shorter than 16 characters', () => {
    expect(
      schema.safeParse({ token: 'short', newPassword: 'password1', confirmPassword: 'password1' })
        .success,
    ).toBe(false);
  });
});
