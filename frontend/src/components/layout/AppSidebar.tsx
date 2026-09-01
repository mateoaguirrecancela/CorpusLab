import { useTranslation } from 'react-i18next';
import { NavLink } from 'react-router';
import { APP_NAVIGATION_ITEMS } from '@/shared/constants/appNavigation';

type AppSidebarProps = Readonly<{
  isCollapsed: boolean;
}>;

export function AppSidebar({ isCollapsed }: AppSidebarProps) {
  const { t } = useTranslation();

  return (
    <aside
      className={[
        'z-30 flex shrink-0 flex-col bg-sidebar',
        'fixed top-[72px] bottom-0 left-0 transition-all duration-200 ease-in-out',
        isCollapsed
          ? '-translate-x-full w-0 overflow-hidden border-r-0 pointer-events-none md:translate-x-0 md:w-[84px] md:border-r md:border-border md:pointer-events-auto'
          : 'translate-x-0 w-[260px] border-r border-border shadow-[var(--shadow-sidebar)] md:shadow-none',
      ].join(' ')}
    >
      <nav className="flex flex-1 flex-col gap-1 px-4 py-4">
        {APP_NAVIGATION_ITEMS.map((item) => {
          const Icon = item.icon;
          return (
            <NavLink
              className={({ isActive }) =>
                [
                  'flex h-11 w-full items-center rounded-lg px-4 text-left text-sm font-medium transition-all duration-200 ease-in-out',
                  isActive
                    ? 'bg-accent text-primary'
                    : 'text-muted-foreground hover:bg-surface-base hover:text-primary',
                ].join(' ')
              }
              end={item.to === '/home'}
              key={item.key}
              to={item.to}
            >
              <Icon className="size-4 shrink-0" />
              <span
                className={[
                  'whitespace-nowrap transition-all duration-200 ease-in-out origin-left',
                  isCollapsed
                    ? 'w-0 max-w-0 opacity-0 pointer-events-none scale-95 ml-0 overflow-hidden'
                    : 'w-auto max-w-[200px] opacity-100 scale-100 ml-4',
                ].join(' ')}
              >
                {t(`home.nav.${item.key}`)}
              </span>
            </NavLink>
          );
        })}
      </nav>
    </aside>
  );
}
