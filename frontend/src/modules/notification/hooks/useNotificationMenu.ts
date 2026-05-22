import { useTranslation } from 'react-i18next';
import { toast } from 'sonner';
import { useToastMessages } from '@/hooks/useToastMessages';
import { getResolvedLanguage } from '@/lib/i18n';
import {
  useMarkAllNotificationsAsReadMutation,
  useMarkNotificationAsReadMutation,
  useNotificationsQuery,
} from '@/modules/notification/hooks/useNotificationQueries';
import {
  getMarkAllNotificationsReadErrorMessage,
  getMarkNotificationReadErrorMessage,
  getNotificationsErrorMessage,
} from '@/modules/notification/services/notificationService';
import { type NotificationItem } from '@/modules/notification/types/notification';
import {
  canMarkAllNotificationsAsRead,
  canMarkNotificationAsRead,
  getNotificationListStatus,
  getNotifications,
  getUnreadCount,
} from '@/modules/notification/utils/notificationMenu';

const NOTIFICATIONS_LIMIT = 10;

export function useNotificationMenu() {
  const { i18n } = useTranslation();
  const {
    data: notificationsData,
    error: notificationsError,
    isError: isNotificationsError,
    isLoading: isNotificationsLoading,
  } = useNotificationsQuery(NOTIFICATIONS_LIMIT);
  const markNotificationAsReadMutation = useMarkNotificationAsReadMutation();
  const markAllNotificationsAsReadMutation = useMarkAllNotificationsAsReadMutation();
  const notifications = getNotifications(notificationsData);
  const unreadCount = getUnreadCount(notificationsData);
  const listStatus = getNotificationListStatus({
    isError: isNotificationsError,
    isLoading: isNotificationsLoading,
    notifications,
  });
  const canMarkAllAsRead = canMarkAllNotificationsAsRead(
    unreadCount,
    markAllNotificationsAsReadMutation.isPending,
  );
  const errorMessage = isNotificationsError ? getNotificationsErrorMessage(notificationsError) : '';

  useToastMessages({
    errorMessage,
    errorToastId: 'notifications-load-error',
  });

  const markNotificationAsRead = (notification: NotificationItem) => {
    if (!canMarkNotificationAsRead(notification.read, markNotificationAsReadMutation.isPending)) {
      return;
    }

    markNotificationAsReadMutation.mutate(notification.id, {
      onError: (error) => {
        toast.error(getMarkNotificationReadErrorMessage(error));
      },
    });
  };

  const markAllNotificationsAsRead = () => {
    if (!canMarkAllAsRead) {
      return;
    }

    markAllNotificationsAsReadMutation.mutate(undefined, {
      onError: (error) => {
        toast.error(getMarkAllNotificationsReadErrorMessage(error));
      },
    });
  };

  return {
    canMarkAllAsRead,
    listStatus,
    locale: getResolvedLanguage(i18n),
    markAllNotificationsAsRead,
    markNotificationAsRead,
    notifications,
    unreadCount,
  };
}
