import { useMemo, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { Bell, PanelLeftClose, PanelLeftOpen } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Link, useNavigate } from 'react-router';
import corpusLabLogo from '@/assets/CorpusLab.png';
import { FeedbackMessage } from '@/components/ui/feedback-message';
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover';
import { Spinner } from '@/components/ui/spinner';
import { getUserInitials } from '@/lib/user';
import { SESSION_AUTH_TOKEN_STORAGE_KEY } from '@/modules/auth/constants/session';
import { PROFILE_QUERY_KEY, useProfileQuery } from '@/modules/auth/hooks/useProfileQuery';
import { getLogoutErrorMessage, logout } from '@/modules/auth/services/authService';
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
  const [notificationActionError, setNotificationActionError] = useState('');
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
      console.error(getLogoutErrorMessage(error));
    } finally {
      localStorage.removeItem(SESSION_AUTH_TOKEN_STORAGE_KEY);
      queryClient.removeQueries({ queryKey: PROFILE_QUERY_KEY });
      queryClient.removeQueries({ queryKey: NOTIFICATIONS_QUERY_KEY });
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
    setNotificationActionError('');

    if (isRead || markNotificationAsReadMutation.isPending) {
      return;
    }

    markNotificationAsReadMutation.mutate(notificationId, {
      onError: (error) => {
        setNotificationActionError(getMarkNotificationReadErrorMessage(error));
      },
    });
  };

  const handleMarkAllAsRead = () => {
    setNotificationActionError('');

    if (markAllNotificationsAsReadMutation.isPending) {
      return;
    }

    markAllNotificationsAsReadMutation.mutate(undefined, {
      onError: (error) => {
        setNotificationActionError(getMarkAllNotificationsReadErrorMessage(error));
      },
    });
  };

  return (
    <header className="flex h-[72px] items-center border-b border-[color:var(--cl-line)] bg-white px-4 sm:px-6">
      <div className="flex items-center gap-3">
        <Link className="inline-flex items-center gap-2" to="/home">
          <img alt="CorpusLab" className="h-10 w-10 rounded-sm object-cover" src={corpusLabLogo} />
          <span className="text-lg font-semibold tracking-tight text-[color:var(--cl-neutral)]">
            {t('common.appName')}
          </span>
        </Link>

        <button
          aria-label={
            isSidebarCollapsed ? t('common.aria.expandSidebar') : t('common.aria.collapseSidebar')
          }
          className="inline-flex items-center justify-center rounded-md border border-transparent p-2 text-[color:var(--cl-primary)] transition hover:border-[color:var(--cl-line)] hover:bg-[color:var(--cl-primary-soft)] cursor-pointer"
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
            className="relative inline-flex items-center justify-center rounded-md border border-transparent p-2 text-[color:var(--cl-primary)] transition hover:border-[color:var(--cl-line)] hover:bg-[color:var(--cl-primary-soft)] cursor-pointer"
          >
            <Bell className="size-5" />
            {unreadCount > 0 && (
              <span
                aria-hidden
                className="absolute right-1 top-1 size-1.5 rounded-full bg-[color:var(--cl-primary)]"
              />
            )}
          </PopoverTrigger>

          <PopoverContent
            align="end"
            className="w-[360px] rounded-xl border border-[color:var(--cl-line)] bg-white p-0"
          >
            <div className="flex items-center justify-between border-b border-[color:var(--cl-line)] px-4 py-3">
              <h3 className="text-sm font-semibold text-[color:var(--cl-neutral)]">
                {t('notification.title')}
              </h3>
              <button
                className="rounded-md px-2 py-1 text-xs font-medium text-[color:var(--cl-primary)] transition hover:bg-[color:var(--cl-primary-soft)] disabled:cursor-default disabled:opacity-50 disabled:bg-white cursor-pointer"
                disabled={unreadCount === 0 || markAllNotificationsAsReadMutation.isPending}
                onClick={handleMarkAllAsRead}
                type="button"
              >
                {t('notification.markAllRead')}
              </button>
            </div>

            <div className="max-h-[360px] overflow-y-auto p-2">
              {isNotificationsLoading && (
                <div className="px-3 py-4 text-sm text-[color:var(--cl-secondary)]">
                  <span className="inline-flex items-center gap-2">
                    <Spinner aria-hidden className="size-4" />
                    {t('notification.loading')}
                  </span>
                </div>
              )}

              {isNotificationsError && (
                <FeedbackMessage
                  className="mx-1 my-2"
                  message={getNotificationsErrorMessage(notificationsError)}
                  variant="error"
                />
              )}

              {!isNotificationsLoading && !isNotificationsError && notifications.length === 0 && (
                <p className="px-3 py-4 text-sm text-[color:var(--cl-secondary)]">
                  {t('notification.empty')}
                </p>
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
                            ? 'border-transparent bg-white hover:bg-[color:var(--cl-primary-soft)]'
                            : 'border-[color:var(--cl-primary)]/30 bg-[color:var(--cl-primary-soft)]/60 hover:bg-[color:var(--cl-primary-soft)]',
                        ].join(' ')}
                      >
                        <p className="text-sm font-semibold text-[color:var(--cl-neutral)]">
                          {getNotificationTitle(notification)}
                        </p>
                        <p className="mt-0.5 text-sm text-[color:var(--cl-secondary)]">
                          {getNotificationDescription(notification)}
                        </p>
                        <p className="mt-1 text-xs text-[color:var(--cl-tertiary)]">
                          {formatNotificationDate(notification.createdAt)}
                        </p>
                      </article>
                    </Link>
                  ))}
                </div>
              )}

              {notificationActionError.length > 0 && (
                <FeedbackMessage
                  className="mx-1 mt-2"
                  message={notificationActionError}
                  variant="error"
                />
              )}
            </div>
          </PopoverContent>
        </Popover>

        <div className="h-8 w-px bg-[color:var(--cl-line)]" />

        <Popover>
          <PopoverTrigger className="group flex items-center gap-3 rounded-lg px-1 py-1 cursor-pointer">
            <div className="hidden text-right leading-tight sm:block">
              <p className="text-sm font-semibold text-[color:var(--cl-primary)] group-hover:underline">
                {userLabel}
              </p>
            </div>
            <div className="flex size-10 items-center justify-center rounded-full border border-[color:var(--cl-line)] bg-[color:var(--cl-primary-soft)] text-sm font-bold text-[color:var(--cl-primary)] group-hover:border-[color:var(--cl-primary)]">
              {userInitials}
            </div>
          </PopoverTrigger>

          <PopoverContent
            align="end"
            className="w-44 rounded-xl border border-[color:var(--cl-line)] bg-white p-1.5"
          >
            <Link
              className="flex h-9 w-full items-center rounded-md px-3 text-left text-sm font-medium text-[color:var(--cl-neutral)] transition hover:bg-[color:var(--cl-primary-soft)]"
              to="/home/profile"
            >
              {t('common.actions.viewProfile')}
            </Link>

            <hr />

            <button
              className="flex h-9 w-full items-center rounded-md px-3 text-left text-sm font-medium text-red-700 transition hover:bg-red-50 disabled:cursor-not-allowed disabled:opacity-70 cursor-pointer"
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
