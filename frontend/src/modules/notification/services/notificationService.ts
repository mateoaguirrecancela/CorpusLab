import { api } from '@/lib/api';
import i18n from '@/lib/i18n';
import { getSessionToken } from '@/modules/auth/services/sessionService';
import {
  type NotificationItem,
  type NotificationListResponse,
} from '@/modules/notification/types/notification';

type NotificationDto = {
  id: number;
  type: NotificationItem['type'];
  read: boolean;
  createdAt: string;
  actorFullName: string | null;
  researchGroupId: number | null;
  researchGroupName: string | null;
  invitationId: number | null;
  projectId: number | null;
  projectName: string | null;
};

type NotificationListResponseDto = {
  notifications: NotificationDto[];
  unreadCount: number;
};

export async function getMyNotifications(limit = 12): Promise<NotificationListResponse> {
  const response = await api.get<NotificationListResponseDto>('/notifications', {
    params: { limit },
  });

  return {
    notifications: response.data.notifications,
    unreadCount: response.data.unreadCount,
  };
}

export async function markNotificationAsRead(notificationId: number): Promise<NotificationItem> {
  const response = await api.post<NotificationDto>(`/notifications/${notificationId}/read`);
  return response.data;
}

export async function markAllNotificationsAsRead(): Promise<void> {
  await api.post('/notifications/read-all');
}

export function subscribeToNotificationEvents(onChange: () => void): () => void {
  const abortController = new AbortController();
  const token = getSessionToken();
  const language = i18n.resolvedLanguage ?? i18n.language ?? 'en';

  void (async () => {
    try {
      const response = await fetch('/api/notifications/stream', {
        headers: {
          Accept: 'text/event-stream',
          'Accept-Language': language,
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
        signal: abortController.signal,
      });

      if (!response.ok || !response.body) {
        return;
      }

      const reader = response.body.getReader();
      const decoder = new TextDecoder();
      let buffer = '';

      while (!abortController.signal.aborted) {
        const { done, value } = await reader.read();
        if (done) {
          break;
        }

        buffer += decoder.decode(value, { stream: true });
        const events = buffer.split('\n\n');
        buffer = events.pop() ?? '';

        for (const rawEvent of events) {
          if (rawEvent.includes('event:notification') || rawEvent.includes('data:changed')) {
            onChange();
          }
        }
      }
    } catch (error) {
      if (!abortController.signal.aborted) {
        console.debug('Notification stream closed', error);
      }
    }
  })();

  return () => abortController.abort();
}

function extractApiErrorMessage(error: unknown, fallbackKey: string): string {
  if (typeof error === 'object' && error !== null && 'response' in error) {
    const response = (error as { response?: { data?: { message?: string; error?: string } } })
      .response;
    return response?.data?.message ?? response?.data?.error ?? i18n.t(fallbackKey);
  }

  return i18n.t(fallbackKey);
}

export function getNotificationsErrorMessage(error: unknown): string {
  return extractApiErrorMessage(error, 'notification.errors.loadFailed');
}

export function getMarkNotificationReadErrorMessage(error: unknown): string {
  return extractApiErrorMessage(error, 'notification.errors.markReadFailed');
}

export function getMarkAllNotificationsReadErrorMessage(error: unknown): string {
  return extractApiErrorMessage(error, 'notification.errors.markAllReadFailed');
}
