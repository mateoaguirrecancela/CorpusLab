import { describe, expect, it } from 'vitest';
import {
  buildNotificationStreamHeaders,
  hasNotificationChangeEvent,
  splitNotificationStreamBuffer,
} from '@/modules/notification/utils/notificationStream';

describe('buildNotificationStreamHeaders', () => {
  it('includes the Authorization header when a token is present', () => {
    expect(buildNotificationStreamHeaders('tok', 'es')).toEqual({
      Accept: 'text/event-stream',
      'Accept-Language': 'es',
      Authorization: 'Bearer tok',
    });
  });

  it('omits the Authorization header when there is no token', () => {
    expect(buildNotificationStreamHeaders(null, 'en')).toEqual({
      Accept: 'text/event-stream',
      'Accept-Language': 'en',
    });
  });
});

describe('splitNotificationStreamBuffer', () => {
  it('splits complete events and keeps the trailing partial event', () => {
    const result = splitNotificationStreamBuffer('event: a\ndata: 1\n\nevent: b\ndata: 2\n\npart');
    expect(result.events).toEqual(['event: a\ndata: 1', 'event: b\ndata: 2']);
    expect(result.remainingBuffer).toBe('part');
  });

  it('returns no events and an empty remainder for an empty buffer', () => {
    const result = splitNotificationStreamBuffer('');
    expect(result.events).toEqual([]);
    expect(result.remainingBuffer).toBe('');
  });
});

describe('hasNotificationChangeEvent', () => {
  it('detects a matching notification/changed event', () => {
    expect(hasNotificationChangeEvent(['event: notification\ndata: changed'])).toBe(true);
  });

  it('ignores events with a different name or data', () => {
    expect(hasNotificationChangeEvent(['event: notification\ndata: other'])).toBe(false);
    expect(hasNotificationChangeEvent(['event: ping\ndata: changed'])).toBe(false);
  });

  it('is false for an empty list', () => {
    expect(hasNotificationChangeEvent([])).toBe(false);
  });
});
