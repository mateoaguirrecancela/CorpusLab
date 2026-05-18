import { useMemo, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { PanelLeftClose, PanelLeftOpen } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Link, useNavigate } from 'react-router';
import { toast } from 'sonner';
import corpusLabLogo from '@/assets/corpuslab.webp';
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover';
import { getUserInitials } from '@/lib/user';
import { useProfileQuery } from '@/modules/auth/hooks/useProfileQuery';
import { getLogoutErrorMessage, logout } from '@/modules/auth/services/authService';
import { clearSession } from '@/modules/auth/services/sessionService';
import { NotificationMenu } from '@/modules/notification/components/NotificationMenu';
import { NOTIFICATIONS_QUERY_KEY } from '@/modules/notification/hooks/useNotificationQueries';

type AppTopbarProps = Readonly<{
  isSidebarCollapsed: boolean;
  onToggleSidebar: () => void;
}>;

export function AppHeader({ isSidebarCollapsed, onToggleSidebar }: AppTopbarProps) {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [isLoggingOut, setIsLoggingOut] = useState(false);
  const { data: profile } = useProfileQuery();

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

  return (
    <header className="fixed inset-x-0 top-0 z-50 flex h-18 items-center border-b border-border bg-surface-header px-4 sm:px-6">
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
        <NotificationMenu />

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
