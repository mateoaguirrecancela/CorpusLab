import { Settings } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { NavLink } from 'react-router';
import { APP_NAVIGATION_ITEMS } from '@/constants/appNavigation';

type AppSidebarProps = {
  isCollapsed: boolean;
};

export function AppSidebar({ isCollapsed }: AppSidebarProps) {
  const { t } = useTranslation();

  return (
    <aside
      className={[
        'z-30 flex shrink-0 flex-col bg-sidebar',
        'fixed top-[72px] bottom-0 left-0 md:relative md:top-auto md:bottom-auto md:left-auto',
        isCollapsed
          ? '-translate-x-full w-0 overflow-hidden border-r-0 pointer-events-none md:translate-x-0 md:w-[84px] md:border-r md:border-[color:var(--cl-line)] md:pointer-events-auto'
          : 'translate-x-0 w-[260px] border-r border-[color:var(--cl-line)] shadow-[0_12px_30px_-22px_rgba(15,23,42,0.7)] md:shadow-none',
      ].join(' ')}
    >
      <nav className="flex flex-1 flex-col gap-1 px-4 py-4">
        {APP_NAVIGATION_ITEMS.map((item) => {
          const Icon = item.icon;
          return (
            <NavLink
              className={({ isActive }) =>
                [
                  'flex h-11 w-full items-center rounded-lg px-4 text-left text-sm font-medium',
                  isCollapsed ? 'justify-center gap-0' : 'gap-3',
                  isActive
                    ? 'bg-[color:var(--cl-primary-soft)] text-[color:var(--cl-primary)]'
                    : 'text-[color:var(--cl-secondary)] hover:bg-white hover:text-[color:var(--cl-primary)]',
                ].join(' ')
              }
              end
              key={item.key}
              to={item.to}
            >
              <Icon className="size-4" />
              {!isCollapsed && <span>{t(`home.nav.${item.key}`)}</span>}
            </NavLink>
          );
        })}
      </nav>

      <div className="border-t border-[color:var(--cl-line)] px-3 py-3">
        <button
          className={[
            'flex h-11 w-full items-center rounded-lg px-3 text-sm font-medium text-[color:var(--cl-secondary)] hover:bg-white hover:text-[color:var(--cl-primary)] cursor-pointer',
            isCollapsed ? 'justify-center gap-0' : 'gap-3',
          ].join(' ')}
          type="button"
        >
          <Settings className="size-4" />
          {!isCollapsed && <span>{t('home.nav.settings')}</span>}
        </button>
      </div>
    </aside>
  );
}
