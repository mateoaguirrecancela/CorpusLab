import { Bell } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover';
import { NotificationList } from '@/modules/notification/components/NotificationList';
import { useNotificationMenu } from '@/modules/notification/hooks/useNotificationMenu';

type NotificationUnreadIndicatorProps = Readonly<{
  unreadCount: number;
}>;

function NotificationUnreadIndicator({ unreadCount }: NotificationUnreadIndicatorProps) {
  if (unreadCount <= 0) {
    return null;
  }

  return <span aria-hidden className="absolute right-1 top-1 size-1.5 rounded-full bg-primary" />;
}

export function NotificationMenu() {
  const {
    canMarkAllAsRead,
    listStatus,
    locale,
    markAllNotificationsAsRead,
    markNotificationAsRead,
    notifications,
    unreadCount,
  } = useNotificationMenu();
  const { t } = useTranslation();

  return (
    <Popover>
      <PopoverTrigger
        aria-label={t('notification.aria.openMenu')}
        className="relative inline-flex cursor-pointer items-center justify-center rounded-md border border-transparent p-2 text-primary transition hover:border-border hover:bg-accent"
      >
        <Bell className="size-5" />
        <NotificationUnreadIndicator unreadCount={unreadCount} />
      </PopoverTrigger>

      <PopoverContent
        align="end"
        className="w-90 rounded-xl border border-border bg-surface-base p-0"
      >
        <div className="flex items-center justify-between border-b border-border px-4 py-3">
          <h3 className="text-sm font-semibold text-foreground">{t('notification.title')}</h3>
          <button
            className="cursor-pointer rounded-md px-2 py-1 text-xs font-medium text-primary transition hover:bg-accent disabled:cursor-default disabled:bg-surface-base disabled:opacity-50"
            disabled={!canMarkAllAsRead}
            onClick={markAllNotificationsAsRead}
            type="button"
          >
            {t('notification.markAllRead')}
          </button>
        </div>

        <div className="max-h-90 overflow-y-auto p-2">
          <NotificationList
            locale={locale}
            notifications={notifications}
            status={listStatus}
            onNotificationClick={markNotificationAsRead}
          />
        </div>
      </PopoverContent>
    </Popover>
  );
}
