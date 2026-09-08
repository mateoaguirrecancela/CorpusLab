import { useTranslation } from 'react-i18next';
import { Outlet } from 'react-router';
import { AppSidebar } from '@/components/layout/AppSidebar';
import { AppHeader } from '@/components/layout/AppHeader';
import { useAppShellStore } from '@/components/layout/useAppShellStore';

export default function AppShell() {
  const { t } = useTranslation();
  const { closeSidebar, isSidebarCollapsed, toggleSidebar } = useAppShellStore();

  return (
    <div className="min-h-screen bg-background">
      <AppHeader isSidebarCollapsed={isSidebarCollapsed} onToggleSidebar={toggleSidebar} />

      <AppSidebar isCollapsed={isSidebarCollapsed} />

      {!isSidebarCollapsed && (
        <button
          aria-label={t('common.aria.closeSidebar')}
          className="fixed inset-0 top-[72px] z-20 bg-overlay-scrim md:hidden"
          onClick={closeSidebar}
          type="button"
        />
      )}

      <div
        className={[
          'flex min-h-screen flex-col pt-[72px] transition-[margin] duration-200',
          isSidebarCollapsed ? 'md:ml-[84px]' : 'md:ml-[260px]',
        ].join(' ')}
      >
        <main className="flex-1 overflow-auto">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
