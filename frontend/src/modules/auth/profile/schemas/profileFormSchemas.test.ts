import { describe, expect, it } from 'vitest';
import { type TFunction } from 'i18next';
import { createProfileSchema } from '@/modules/auth/profile/schemas/profileFormSchemas';

const t = ((key: string) => key) as TFunction;

function isoDateYearsAgo(years: number): string {
  const date = new Date();
  date.setFullYear(date.getFullYear() - years);
  return date.toISOString().slice(0, 10);
}

describe('createProfileSchema', () => {
  const schema = createProfileSchema(t);

  const validPayload = {
    firstName: 'Jane',
    lastName: 'Doe',
    birth: isoDateYearsAgo(20),
    gender: 'FEMALE',
    countryCode: 'ES',
    city: 'Coruna',
  };

  it('accepts a fully valid profile', () => {
    expect(schema.safeParse(validPayload).success).toBe(true);
  });

  it('rejects an underage birth date', () => {
    expect(schema.safeParse({ ...validPayload, birth: isoDateYearsAgo(5) }).success).toBe(false);
  });

  it('rejects a missing required field', () => {
    expect(schema.safeParse({ ...validPayload, firstName: '' }).success).toBe(false);
  });

  it('rejects an empty gender', () => {
    expect(schema.safeParse({ ...validPayload, gender: '' }).success).toBe(false);
  });
});
