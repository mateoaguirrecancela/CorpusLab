import { Settings } from 'lucide-react'
import { NavLink } from 'react-router'
import { APP_NAVIGATION_ITEMS } from '@/constants/appNavigation'

type AppSidebarProps = {
  isCollapsed: boolean
}

export function AppSidebar({ isCollapsed }: AppSidebarProps) {
  return (
    <aside className={[
      'hidden shrink-0 border-r border-[color:var(--cl-line)] bg-[#eef1fb] transition-[width] duration-200 md:flex md:flex-col',
      isCollapsed ? 'w-[84px]' : 'w-[220px]',
    ].join(' ')}>
      <nav className="flex flex-1 flex-col gap-1 px-3 py-4">
        {APP_NAVIGATION_ITEMS.map((item) => {
          const Icon = item.icon
          return (
            <NavLink
              className={({ isActive }) => [
                'flex h-11 w-full items-center rounded-lg px-3 text-left text-sm font-medium transition',
                isCollapsed ? 'justify-center gap-0' : 'gap-3',
                isActive
                  ? 'bg-[color:var(--cl-primary-soft)] text-[color:var(--cl-primary)]'
                  : 'text-[color:var(--cl-secondary)] hover:bg-white/70 hover:text-[color:var(--cl-primary)]',
              ].join(' ')}
              key={item.key}
              to={item.to}
            >
              <Icon className="size-4" />
              {!isCollapsed && <span>{item.label}</span>}
            </NavLink>
          )
        })}
      </nav>

      <div className="border-t border-[color:var(--cl-line)] px-3 py-3">
        <button className={[
          'flex h-11 w-full items-center rounded-lg px-3 text-sm font-medium text-[color:var(--cl-secondary)] hover:bg-white/70 hover:text-[color:var(--cl-primary)] cursor-pointer',
          isCollapsed ? 'justify-center gap-0' : 'gap-3',
        ].join(' ')} type="button">
          <Settings className="size-4" />
          {!isCollapsed && <span>Settings</span>}
        </button>
      </div>
    </aside>
  )
}
