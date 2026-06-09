import { useTranslation } from 'react-i18next';
import { getResolvedLanguage } from '@/app/config/i18n';
import {
  useMarkNotificationAsReadMutation,
  useNotificationsQuery,
} from '@/modules/notification/hooks/useNotificationQueries';
import { type NotificationItem } from '@/modules/notification/types/notification';
import {
  getNotificationListStatus,
  getNotifications,
  type NotificationListStatus,
} from '@/modules/notification/utils/notificationMenu';

type ActivityFeedState = Readonly<{
  listStatus: NotificationListStatus;
  locale: string;
  notifications: NotificationItem[];
  markNotificationAsRead: (notification: NotificationItem) => void;
}>;

export function useActivityFeed(): ActivityFeedState {
  const { i18n } = useTranslation();
  const notificationsQuery = useNotificationsQuery(10);
  const markAsReadMutation = useMarkNotificationAsReadMutation();
  const notifications = getNotifications(notificationsQuery.data);

  return {
    listStatus: getNotificationListStatus({
      isError: notificationsQuery.isError,
      isLoading: notificationsQuery.isLoading,
      notifications,
    }),
    locale: getResolvedLanguage(i18n),
    notifications,
    markNotificationAsRead: (notification) => {
      if (notification.read) {
        return;
      }

      markAsReadMutation.mutate(notification.id);
    },
  };
}
