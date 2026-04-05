import { useEffect, useMemo, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { Bell, PanelLeftClose, PanelLeftOpen } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Link, useNavigate } from 'react-router';
import { toast } from 'sonner';
import corpusLabLogo from '@/assets/CorpusLab.png';
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover';
import { Spinner } from '@/components/ui/spinner';
import { getUserInitials } from '@/lib/user';
import { useProfileQuery } from '@/modules/auth/hooks/useProfileQuery';
import { getLogoutErrorMessage, logout } from '@/modules/auth/services/authService';
import { clearSession } from '@/modules/auth/services/sessionService';
import {
  NOTIFICATIONS_QUERY_KEY,
  useMarkAllNotificationsAsReadMutation,
  useMarkNotificationAsReadMutation,
  useNotificationsQuery,
} from '@/modules/notification/hooks/useNotificationQueries';
import {
  getNotificationsErrorMessage,
  getMarkAllNotificationsReadErrorMessage,
  getMarkNotificationReadErrorMessage,
} from '@/modules/notification/services/notificationService';
import { type NotificationItem } from '@/modules/notification/types/notification';

type AppTopbarProps = Readonly<{
  isSidebarCollapsed: boolean;
  onToggleSidebar: () => void;
}>;

export function AppHeader({ isSidebarCollapsed, onToggleSidebar }: AppTopbarProps) {
  const notificationsLimit = 12;
  const { i18n, t } = useTranslation();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [isLoggingOut, setIsLoggingOut] = useState(false);
  const { data: profile } = useProfileQuery();
  const {
    data: notificationsData,
    isLoading: isNotificationsLoading,
    isError: isNotificationsError,
    error: notificationsError,
  } = useNotificationsQuery(notificationsLimit);
  const markNotificationAsReadMutation = useMarkNotificationAsReadMutation(notificationsLimit);
  const markAllNotificationsAsReadMutation =
    useMarkAllNotificationsAsReadMutation(notificationsLimit);

  const notifications = notificationsData?.notifications ?? [];
  const unreadCount = notificationsData?.unreadCount ?? 0;

  useEffect(() => {
    if (isNotificationsError) {
      toast.error(getNotificationsErrorMessage(notificationsError), {
        id: 'notifications-load-error',
      });
    }
  }, [isNotificationsError, notificationsError]);

  const userLabel = useMemo(() => {
    const firstName = profile?.firstName?.trim() ?? '';
    const lastName = profile?.lastName?.trim() ?? '';

    const fullName = `${firstName} ${lastName}`.trim();
    return fullName.length > 0 ? fullName : t('common.user');
  }, [profile, t]);

  const userInitials = useMemo(() => {
    return getUserInitials(profile ?? {});
  }, [profile]);

  const handleLogout = async () => {
    if (isLoggingOut) {
      return;
    }

    setIsLoggingOut(true);

    try {
      await logout();
    } catch (error) {
      toast.error(getLogoutErrorMessage(error));
    } finally {
      clearSession(queryClient, [NOTIFICATIONS_QUERY_KEY]);
      navigate('/auth/login', { replace: true });
      setIsLoggingOut(false);
    }
  };

  const formatNotificationDate = (createdAt: string) => {
    const locale = i18n.resolvedLanguage ?? i18n.language ?? 'en';

    return new Intl.DateTimeFormat(locale, {
      dateStyle: 'short',
      timeStyle: 'short',
    }).format(new Date(createdAt));
  };

  const getNotificationDestination = (notification: NotificationItem) => {
    if (
      notification.type === 'RESEARCH_GROUP_INVITATION_ACCEPTED' &&
      notification.researchGroupId !== null
    ) {
      return `/home/research-groups/${notification.researchGroupId}`;
    }

    return '/home/research-groups';
  };

  const getNotificationTitle = (notification: NotificationItem) => {
    return t(`notification.types.${notification.type}.title`);
  };

  const getNotificationDescription = (notification: NotificationItem) => {
    return t(`notification.types.${notification.type}.description`, {
      actor: notification.actorFullName ?? t('notification.unknownActor'),
      group: notification.researchGroupName ?? t('notification.fallbackGroup'),
    });
  };

  const handleNotificationClick = (notificationId: number, isRead: boolean) => {
    if (isRead || markNotificationAsReadMutation.isPending) {
      return;
    }

    markNotificationAsReadMutation.mutate(notificationId, {
      onError: (error) => {
        toast.error(getMarkNotificationReadErrorMessage(error));
      },
    });
  };

  const handleMarkAllAsRead = () => {
    if (markAllNotificationsAsReadMutation.isPending) {
      return;
    }

    markAllNotificationsAsReadMutation.mutate(undefined, {
      onError: (error) => {
        toast.error(getMarkAllNotificationsReadErrorMessage(error));
      },
    });
  };

  return (
    <header className="fixed inset-x-0 top-0 z-50 flex h-[72px] items-center border-b border-border bg-surface-header px-4 sm:px-6">
      <div className="flex items-center gap-3">
        <Link className="inline-flex items-center gap-2" to="/home">
          <img alt="CorpusLab" className="h-10 w-10 rounded-sm object-cover" src={corpusLabLogo} />
          <span className="text-lg font-semibold tracking-tight text-foreground">
            {t('common.appName')}
          </span>
        </Link>

        <button
          aria-label={
            isSidebarCollapsed ? t('common.aria.expandSidebar') : t('common.aria.collapseSidebar')
          }
          className="inline-flex items-center justify-center rounded-md border border-transparent p-2 text-primary transition hover:border-border hover:bg-accent cursor-pointer"
          onClick={onToggleSidebar}
          type="button"
        >
          {isSidebarCollapsed ? (
            <PanelLeftOpen className="size-5" />
          ) : (
            <PanelLeftClose className="size-5" />
          )}
        </button>
      </div>

      <div className="ml-auto flex items-center justify-end gap-5">
        <Popover>
          <PopoverTrigger
            aria-label={t('notification.aria.openMenu')}
            className="relative inline-flex items-center justify-center rounded-md border border-transparent p-2 text-primary transition hover:border-border hover:bg-accent cursor-pointer"
          >
            <Bell className="size-5" />
            {unreadCount > 0 && (
              <span
                aria-hidden
                className="absolute right-1 top-1 size-1.5 rounded-full bg-primary"
              />
            )}
          </PopoverTrigger>

          <PopoverContent
            align="end"
            className="w-[360px] rounded-xl border border-border bg-surface-base p-0"
          >
            <div className="flex items-center justify-between border-b border-border px-4 py-3">
              <h3 className="text-sm font-semibold text-foreground">{t('notification.title')}</h3>
              <button
                className="rounded-md px-2 py-1 text-xs font-medium text-primary transition hover:bg-accent disabled:cursor-default disabled:opacity-50 disabled:bg-surface-base cursor-pointer"
                disabled={unreadCount === 0 || markAllNotificationsAsReadMutation.isPending}
                onClick={handleMarkAllAsRead}
                type="button"
              >
                {t('notification.markAllRead')}
              </button>
            </div>

            <div className="max-h-[360px] overflow-y-auto p-2">
              {isNotificationsLoading && (
                <div className="px-3 py-4 text-sm text-muted-foreground">
                  <span className="inline-flex items-center gap-2">
                    <Spinner aria-hidden className="size-4" />
                    {t('notification.loading')}
                  </span>
                </div>
              )}

              {!isNotificationsLoading && !isNotificationsError && notifications.length === 0 && (
                <p className="px-3 py-4 text-sm text-muted-foreground">{t('notification.empty')}</p>
              )}

              {!isNotificationsLoading && !isNotificationsError && notifications.length > 0 && (
                <div className="space-y-1">
                  {notifications.map((notification) => (
                    <Link
                      className="block rounded-lg"
                      key={notification.id}
                      onClick={() => handleNotificationClick(notification.id, notification.read)}
                      to={getNotificationDestination(notification)}
                    >
                      <article
                        className={[
                          'rounded-lg border px-3 py-2 transition',
                          notification.read
                            ? 'border-transparent bg-surface-base hover:bg-accent'
                            : 'border-notification-unread-border bg-notification-unread-bg hover:bg-accent',
                        ].join(' ')}
                      >
                        <p className="text-sm font-semibold text-foreground">
                          {getNotificationTitle(notification)}
                        </p>
                        <p className="mt-0.5 text-sm text-muted-foreground">
                          {getNotificationDescription(notification)}
                        </p>
                        <p className="mt-1 text-xs text-muted-foreground">
                          {formatNotificationDate(notification.createdAt)}
                        </p>
                      </article>
                    </Link>
                  ))}
                </div>
              )}
            </div>
          </PopoverContent>
        </Popover>

        <div className="h-8 w-px bg-border" />

        <Popover>
          <PopoverTrigger className="group flex items-center gap-3 rounded-lg px-1 py-1 cursor-pointer">
            <div className="hidden text-right leading-tight sm:block">
              <p className="text-sm font-semibold text-primary group-hover:underline">
                {userLabel}
              </p>
            </div>
            <div className="flex size-10 items-center justify-center rounded-full border border-border bg-accent text-sm font-bold text-primary group-hover:border-primary">
              {userInitials}
            </div>
          </PopoverTrigger>

          <PopoverContent
            align="end"
            className="w-44 rounded-xl border border-border bg-surface-base p-1.5"
          >
            <Link
              className="flex h-9 w-full items-center rounded-md px-3 text-left text-sm font-medium text-foreground transition hover:bg-accent"
              to="/home/profile"
            >
              {t('common.actions.viewProfile')}
            </Link>

            <hr />

            <button
              className="flex h-9 w-full items-center rounded-md px-3 text-left text-sm font-medium text-destructive transition hover:bg-danger-soft disabled:cursor-not-allowed disabled:opacity-70 cursor-pointer"
              disabled={isLoggingOut}
              onClick={handleLogout}
              type="button"
            >
              {isLoggingOut ? t('common.actions.loggingOut') : t('common.actions.logout')}
            </button>
          </PopoverContent>
        </Popover>
      </div>
    </header>
  );
}
