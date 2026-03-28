import { useState } from 'react'
import { Outlet } from 'react-router'
import { AppFooter } from '@/components/layout/AppFooter'
import { AppSidebar } from '@/components/layout/AppSidebar'
import { AppHeader } from '@/components/layout/AppHeader'

export default function HomeLayout() {
  const [isSidebarCollapsed, setIsSidebarCollapsed] = useState(false)

  return (
    <div className="min-h-screen overflow-hidden bg-[#f3f5ff]">
      <div className="flex min-h-screen flex-col">
        <AppHeader
          isSidebarCollapsed={isSidebarCollapsed}
          onToggleSidebar={() => setIsSidebarCollapsed((current) => !current)}
        />

        <div className="flex min-h-0 flex-1">
          <AppSidebar isCollapsed={isSidebarCollapsed} />

          {!isSidebarCollapsed && (
            <button
              aria-label="Close sidebar"
              className="fixed inset-0 top-[72px] z-20 bg-slate-900/20 md:hidden"
              onClick={() => setIsSidebarCollapsed(true)}
              type="button"
            />
          )}

          <div className="flex min-h-0 min-w-0 flex-1 flex-col">
            <main className="min-h-0 flex-1 overflow-auto">
              <Outlet />
            </main>

            <AppFooter />
          </div>
        </div>
      </div>
    </div>
  )
}
