import { describe, expect, it } from 'vitest';
import { getUserInitials } from '@/shared/utils/user';

describe('getUserInitials', () => {
  it('builds initials from first and last name', () => {
    expect(getUserInitials({ firstName: 'jane', lastName: 'doe' })).toBe('JD');
  });

  it('falls back to the email initial when names are missing', () => {
    expect(getUserInitials({ firstName: null, lastName: null, email: 'admin@corpuslab.com' })).toBe(
      'A',
    );
  });

  it('falls back to "U" when nothing is available', () => {
    expect(getUserInitials({})).toBe('U');
    expect(getUserInitials({ firstName: '  ', lastName: '  ', email: '  ' })).toBe('U');
  });

  it('uses just one available name when the other is missing', () => {
    expect(getUserInitials({ firstName: 'jane', lastName: null })).toBe('J');
  });
});
