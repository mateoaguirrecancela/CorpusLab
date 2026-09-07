import { describe, expect, it } from 'vitest';
import { cn } from '@/shared/utils/cn';

describe('cn', () => {
  it('joins class names, dropping falsy values', () => {
    const isActive = false;
    expect(cn('a', isActive && 'b', undefined, 'c')).toBe('a c');
  });

  it('resolves conflicting tailwind utility classes, keeping the last one', () => {
    expect(cn('p-2', 'p-4')).toBe('p-4');
  });
});
