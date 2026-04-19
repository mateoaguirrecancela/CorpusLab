import { api } from '@/lib/api';
import i18n from '@/lib/i18n';
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
