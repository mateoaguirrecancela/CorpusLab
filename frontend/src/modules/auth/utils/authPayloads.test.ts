import { describe, expect, it } from 'vitest';
import {
  toForgotPasswordPayload,
  toLoginPayload,
  toOAuthExchangePayload,
  toProfileUpdatePayload,
  toResetPasswordPayload,
  toSignupPayload,
} from '@/modules/auth/utils/authPayloads';

describe('toSignupPayload', () => {
  it('trims text fields and keeps password/birth untouched', () => {
    const payload = toSignupPayload({
      email: '  a@b.com  ',
      firstName: ' John ',
      lastName: ' Doe ',
      birth: '2000-01-01',
      gender: 'MALE',
      countryCode: 'ES',
      city: ' Coruna ',
      password: '  secret1  ',
    });

    expect(payload).toEqual({
      email: 'a@b.com',
      firstName: 'John',
      lastName: 'Doe',
      birth: '2000-01-01',
      gender: 'MALE',
      countryCode: 'ES',
      city: 'Coruna',
      password: '  secret1  ',
    });
  });

  it('sends undefined gender when empty', () => {
    const payload = toSignupPayload({
      email: 'a@b.com',
      firstName: 'John',
      lastName: 'Doe',
      birth: '2000-01-01',
      gender: '',
      countryCode: 'ES',
      city: 'Coruna',
      password: 'secret123',
    });

    expect(payload.gender).toBeUndefined();
  });
});

describe('toLoginPayload', () => {
  it('trims email but not password', () => {
    expect(toLoginPayload({ email: ' a@b.com ', password: ' pw ' })).toEqual({
      email: 'a@b.com',
      password: ' pw ',
    });
  });
});

describe('toForgotPasswordPayload', () => {
  it('trims the email', () => {
    expect(toForgotPasswordPayload({ email: ' a@b.com ' })).toEqual({ email: 'a@b.com' });
  });
});

describe('toResetPasswordPayload', () => {
  it('trims the token but keeps the password intact', () => {
    expect(toResetPasswordPayload({ token: ' tok ', newPassword: ' pw ' })).toEqual({
      token: 'tok',
      newPassword: ' pw ',
    });
  });
});

describe('toOAuthExchangePayload', () => {
  it('trims the code', () => {
    expect(toOAuthExchangePayload(' abc123 ')).toEqual({ code: 'abc123' });
  });
});

describe('toProfileUpdatePayload', () => {
  it('trims first and last name and keeps other fields', () => {
    const payload = toProfileUpdatePayload({
      firstName: ' John ',
      lastName: ' Doe ',
      birth: '2000-01-01',
      gender: 'MALE',
      countryCode: 'ES',
      city: 'Coruna',
    });

    expect(payload).toEqual({
      firstName: 'John',
      lastName: 'Doe',
      birth: '2000-01-01',
      gender: 'MALE',
      countryCode: 'ES',
      city: 'Coruna',
    });
  });
});
