import { Bell } from 'lucide-react';

import { useTranslation } from 'react-i18next';

import { NotificationList } from '@/modules/notification/components/NotificationList';

import { type NotificationItem } from '@/modules/notification/types/notification';

import { type NotificationListStatus } from '@/modules/notification/utils/notificationMenu';

type ActivityFeedAsideProps = Readonly<{
  listStatus: NotificationListStatus;

  locale: string;

  notifications: NotificationItem[];

  onNotificationClick: (notification: NotificationItem) => void;
}>;

export function ActivityFeedAside({
  listStatus,

  locale,

  notifications,

  onNotificationClick,
}: ActivityFeedAsideProps) {
  const { t } = useTranslation();

  return (
    <aside className="flex min-h-0 flex-col overflow-hidden rounded-xl border border-border bg-surface-base p-5 xl:h-full xl:sticky xl:top-24">
      <header className="mb-4 flex items-center justify-between gap-3">
        <h2 className="text-lg font-black leading-tight text-primary">
          {t('home.dashboard.activity.title')}
        </h2>

        <span className="inline-flex size-10 shrink-0 items-center justify-center rounded-lg bg-primary/10 text-primary">
          <Bell className="size-5" />
        </span>
      </header>

      <div className="-mx-2 min-h-0 flex-1 overflow-y-auto px-2">
        <NotificationList
          locale={locale}
          notifications={notifications}
          status={listStatus}
          onNotificationClick={onNotificationClick}
        />
      </div>
    </aside>
  );
}
