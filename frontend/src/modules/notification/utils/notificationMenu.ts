import {
  type NotificationItem,
  type NotificationListResponse,
} from '@/modules/notification/types/notification';

export type NotificationListStatus = 'empty' | 'error' | 'loading' | 'ready';

type NotificationListStatusInput = {
  isError: boolean;
  isLoading: boolean;
  notifications: readonly NotificationItem[];
};

export function getNotifications(
  response: NotificationListResponse | undefined,
): NotificationItem[] {
  return response?.notifications ?? [];
}

export function getUnreadCount(response: NotificationListResponse | undefined): number {
  return response?.unreadCount ?? 0;
}

export function getNotificationListStatus({
  isError,
  isLoading,
  notifications,
}: NotificationListStatusInput): NotificationListStatus {
  if (isLoading) {
    return 'loading';
  }

  if (isError) {
    return 'error';
  }

  return notifications.length > 0 ? 'ready' : 'empty';
}

export function canMarkNotificationAsRead(isRead: boolean, isPending: boolean): boolean {
  return !isRead && !isPending;
}

export function canMarkAllNotificationsAsRead(unreadCount: number, isPending: boolean): boolean {
  return unreadCount > 0 && !isPending;
}
