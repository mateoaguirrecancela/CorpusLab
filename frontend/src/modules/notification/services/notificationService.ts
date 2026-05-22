import { api } from '@/lib/api';
import { extractTranslatedApiErrorMessage } from '@/lib/apiErrors';
import i18n, { getResolvedLanguage } from '@/lib/i18n';
import { getSessionToken } from '@/modules/auth/services/sessionService';
import {
  type NotificationItem,
  type NotificationListResponse,
} from '@/modules/notification/types/notification';
import {
  buildNotificationStreamHeaders,
  hasNotificationChangeEvent,
  splitNotificationStreamBuffer,
} from '@/modules/notification/utils/notificationStream';

export async function getMyNotifications(limit = 10): Promise<NotificationListResponse> {
  const response = await api.get<NotificationListResponse>('/notifications', {
    params: { limit },
  });

  return response.data;
}

export async function markNotificationAsRead(notificationId: number): Promise<NotificationItem> {
  const response = await api.post<NotificationItem>(`/notifications/${notificationId}/read`);
  return response.data;
}

export async function markAllNotificationsAsRead(): Promise<void> {
  await api.post('/notifications/read-all');
}

export function subscribeToNotificationEvents(onChange: () => void): () => void {
  const abortController = new AbortController();
  const token = getSessionToken();
  const language = getResolvedLanguage(i18n);

  void (async () => {
    try {
      const response = await fetch('/api/notifications/stream', {
        headers: buildNotificationStreamHeaders(token, language),
        signal: abortController.signal,
      });

      if (!response.ok || !response.body) {
        return;
      }

      await readNotificationEventStream(response.body, abortController.signal, onChange);
    } catch {
      // The stream is best-effort; polling/react-query invalidation remains the recovery path.
    }
  })();

  return () => abortController.abort();
}

async function readNotificationEventStream(
  stream: ReadableStream<Uint8Array>,
  signal: AbortSignal,
  onChange: () => void,
): Promise<void> {
  const reader = stream.getReader();
  const decoder = new TextDecoder();
  let buffer = '';

  while (!signal.aborted) {
    const { done, value } = await reader.read();

    if (done) {
      return;
    }

    buffer += decoder.decode(value, { stream: true });
    const { events, remainingBuffer } = splitNotificationStreamBuffer(buffer);
    buffer = remainingBuffer;

    if (hasNotificationChangeEvent(events)) {
      onChange();
    }
  }
}

export function getNotificationsErrorMessage(error: unknown): string {
  return extractTranslatedApiErrorMessage(error, 'notification.errors.loadFailed');
}

export function getMarkNotificationReadErrorMessage(error: unknown): string {
  return extractTranslatedApiErrorMessage(error, 'notification.errors.markReadFailed');
}

export function getMarkAllNotificationsReadErrorMessage(error: unknown): string {
  return extractTranslatedApiErrorMessage(error, 'notification.errors.markAllReadFailed');
}
