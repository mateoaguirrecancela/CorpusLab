import { useMemo, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { Bell, PanelLeftClose, PanelLeftOpen } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Link, useNavigate } from 'react-router';
import corpusLabLogo from '@/assets/CorpusLab.png';
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover';
import { getUserInitials } from '@/lib/user';
import { SESSION_AUTH_TOKEN_STORAGE_KEY } from '@/modules/auth/constants/session';
import { PROFILE_QUERY_KEY, useProfileQuery } from '@/modules/auth/hooks/useProfileQuery';
import { getLogoutErrorMessage, logout } from '@/modules/auth/services/authService';

type AppTopbarProps = {
  isSidebarCollapsed: boolean;
  onToggleSidebar: () => void;
};

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
      console.error(getLogoutErrorMessage(error));
    } finally {
      localStorage.removeItem(SESSION_AUTH_TOKEN_STORAGE_KEY);
      queryClient.removeQueries({ queryKey: PROFILE_QUERY_KEY });
      navigate('/auth/login', { replace: true });
      setIsLoggingOut(false);
    }
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
        <button
          className="items-center justify-center rounded-md border border-transparent p-2 text-[color:var(--cl-primary)] transition hover:border-[color:var(--cl-line)] hover:bg-[color:var(--cl-primary-soft)] cursor-pointer"
          type="button"
        >
          <Bell className="size-5" />
        </button>

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
