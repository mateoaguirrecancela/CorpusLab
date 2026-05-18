import { useTranslation } from 'react-i18next';
import { Spinner } from '@/components/ui/spinner';
import { type NotificationItem } from '@/modules/notification/types/notification';
import {
  formatNotificationDate,
  getNotificationDescription,
  getNotificationTitle,
} from '@/modules/notification/utils/notificationDisplay';
import { type NotificationListStatus } from '@/modules/notification/utils/notificationMenu';

type NotificationListProps = Readonly<{
  locale: string;
  notifications: NotificationItem[];
  status: NotificationListStatus;
  onNotificationClick: (notification: NotificationItem) => void;
}>;

type NotificationListItemProps = Readonly<{
  locale: string;
  notification: NotificationItem;
  onClick: (notification: NotificationItem) => void;
}>;

function NotificationLoadingState() {
  const { t } = useTranslation();

  return (
    <div className="px-3 py-4 text-sm text-muted-foreground">
      <span className="inline-flex items-center gap-2">
        <Spinner aria-hidden className="size-4" />
        {t('notification.loading')}
      </span>
    </div>
  );
}

function NotificationEmptyState() {
  const { t } = useTranslation();

  return <p className="px-3 py-4 text-sm text-muted-foreground">{t('notification.empty')}</p>;
}

function getNotificationCardClassName(isRead: boolean): string {
  const toneClassName = isRead
    ? 'border-transparent bg-surface-base hover:bg-accent'
    : 'border-notification-unread-border bg-notification-unread-bg hover:bg-accent';

  return `rounded-lg border px-3 py-2 transition ${toneClassName}`;
}

function NotificationListItem({ locale, notification, onClick }: NotificationListItemProps) {
  const { t } = useTranslation();

  return (
    <button
      className="block w-full rounded-lg text-left"
      onClick={() => onClick(notification)}
      type="button"
    >
      <article className={getNotificationCardClassName(notification.read)}>
        <p className="text-sm font-semibold text-foreground">
          {getNotificationTitle(notification, t)}
        </p>
        <p className="mt-0.5 text-sm text-muted-foreground">
          {getNotificationDescription(notification, t)}
        </p>
        <p className="mt-1 text-xs text-muted-foreground">
          {formatNotificationDate(notification.createdAt, locale)}
        </p>
      </article>
    </button>
  );
}

export function NotificationList({
  locale,
  notifications,
  status,
  onNotificationClick,
}: NotificationListProps) {
  if (status === 'loading') {
    return <NotificationLoadingState />;
  }

  if (status === 'empty') {
    return <NotificationEmptyState />;
  }

  if (status === 'error') {
    return null;
  }

  return (
    <div className="space-y-1">
      {notifications.map((notification) => (
        <NotificationListItem
          key={notification.id}
          locale={locale}
          notification={notification}
          onClick={onNotificationClick}
        />
      ))}
    </div>
  );
}
