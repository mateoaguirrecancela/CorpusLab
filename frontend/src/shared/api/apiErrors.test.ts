import { describe, expect, it } from 'vitest';
import { extractApiErrorMessage } from '@/shared/api/apiErrors';

describe('extractApiErrorMessage', () => {
  it('prefers the response body message', () => {
    const error = { response: { data: { message: 'Invalid credentials' } } };
    expect(extractApiErrorMessage(error, 'fallback')).toBe('Invalid credentials');
  });

  it('falls back to the response body error field when there is no message', () => {
    const error = { response: { data: { error: 'Bad Request' } } };
    expect(extractApiErrorMessage(error, 'fallback')).toBe('Bad Request');
  });

  it('falls back to the given message when the response has no body fields', () => {
    const error = { response: { data: {} } };
    expect(extractApiErrorMessage(error, 'fallback')).toBe('fallback');
  });

  it('falls back to the given message when the response has no data', () => {
    const error = { response: {} };
    expect(extractApiErrorMessage(error, 'fallback')).toBe('fallback');
  });

  it('falls back to the given message for a non-axios error object', () => {
    expect(extractApiErrorMessage(new Error('network error'), 'fallback')).toBe('fallback');
  });

  it('falls back to the given message for a non-object error', () => {
    expect(extractApiErrorMessage('string error', 'fallback')).toBe('fallback');
    expect(extractApiErrorMessage(null, 'fallback')).toBe('fallback');
    expect(extractApiErrorMessage(undefined, 'fallback')).toBe('fallback');
  });
});
