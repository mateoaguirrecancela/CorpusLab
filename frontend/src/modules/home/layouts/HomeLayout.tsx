import { useState } from 'react'
import { Outlet } from 'react-router'
import { AppFooter } from '@/components/layout/AppFooter'
import { AppSidebar } from '@/components/layout/AppSidebar'
import { AppHeader } from '@/components/layout/AppHeader'

export default function HomeLayout() {
  const [isSidebarCollapsed, setIsSidebarCollapsed] = useState(false)

  return (
    <div className="min-h-screen overflow-hidden rounded-xl border border-[color:var(--cl-line)] bg-[#f3f5ff] shadow-[0_24px_50px_-45px_rgba(15,23,42,0.6)]">
      <div className="flex min-h-screen flex-col">
        <AppHeader
          isSidebarCollapsed={isSidebarCollapsed}
          onToggleSidebar={() => setIsSidebarCollapsed((current) => !current)}
        />

        <div className="flex min-h-0 flex-1">
          <AppSidebar isCollapsed={isSidebarCollapsed} />

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
