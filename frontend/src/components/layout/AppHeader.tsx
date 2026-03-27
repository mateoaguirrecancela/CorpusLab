import { useEffect, useMemo, useState } from 'react'
import { Bell, FlaskConical, PanelLeftClose, PanelLeftOpen, Search } from 'lucide-react'
import { Link, useNavigate } from 'react-router'
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover'
import { getUserInitials } from '@/lib/user'
import { SESSION_AUTH_TOKEN_STORAGE_KEY, SESSION_USER_STORAGE_KEY } from '@/modules/auth/constants/session'
import { getLogoutErrorMessage, logout } from '@/modules/auth/services/authService'

type AppTopbarProps = {
  isSidebarCollapsed: boolean
  onToggleSidebar: () => void
}

export function AppHeader({ isSidebarCollapsed, onToggleSidebar }: AppTopbarProps) {
  const navigate = useNavigate()
  const [isLoggingOut, setIsLoggingOut] = useState(false)
  const [sessionUser, setSessionUser] = useState<{
    firstName?: string
    lastName?: string
    email?: string
  } | null>(null)

  const readSessionUser = () => {
    try {
      const storedValue = localStorage.getItem(SESSION_USER_STORAGE_KEY)

      if (!storedValue) {
        return null
      }

      const parsedValue = JSON.parse(storedValue) as {
        firstName?: string
        lastName?: string
        email?: string
      }

      return parsedValue
    } catch {
      return null
    }
  }

  useEffect(() => {
    const syncSessionUser = () => {
      setSessionUser(readSessionUser())
    }

    syncSessionUser()
    window.addEventListener('session-user-updated', syncSessionUser)

    return () => {
      window.removeEventListener('session-user-updated', syncSessionUser)
    }
  }, [])

  const userLabel = useMemo(() => {
    const firstName = sessionUser?.firstName?.trim() ?? ''
    const lastName = sessionUser?.lastName?.trim() ?? ''

    const fullName = `${firstName} ${lastName}`.trim()
    return fullName.length > 0 ? fullName : 'User'
  }, [sessionUser])

  const userInitials = useMemo(() => {
    return getUserInitials(sessionUser ?? {})
  }, [sessionUser])

  const handleLogout = async () => {
    if (isLoggingOut) {
      return
    }

    setIsLoggingOut(true)

    try {
      await logout()
    } catch (error) {
      console.error(getLogoutErrorMessage(error))
    } finally {
      localStorage.removeItem(SESSION_USER_STORAGE_KEY)
      localStorage.removeItem(SESSION_AUTH_TOKEN_STORAGE_KEY)
      window.dispatchEvent(new Event('session-user-updated'))
      navigate('/auth/login', { replace: true })
      setIsLoggingOut(false)
    }
  }

  return (
    <header className="grid h-[72px] grid-cols-3 items-center border-b border-[color:var(--cl-line)] bg-white px-4 sm:px-6">
      <div className="flex items-center gap-3">
        <Link className="inline-flex items-center gap-2" to="/home">
          <span className="flex size-7 items-center justify-center rounded-sm border border-[color:var(--cl-primary)] bg-[color:var(--cl-primary)] text-white">
            <FlaskConical className="size-4" />
          </span>
          <span className="text-lg font-semibold tracking-tight text-[color:var(--cl-neutral)]">CorpusLab</span>
        </Link>

        <button
          aria-label={isSidebarCollapsed ? 'Expand sidebar' : 'Collapse sidebar'}
          className="hidden items-center justify-center rounded-md border border-transparent p-2 text-[color:var(--cl-primary)] transition hover:border-[color:var(--cl-line)] hover:bg-[color:var(--cl-primary-soft)] md:inline-flex cursor-pointer"
          onClick={onToggleSidebar}
          type="button"
        >
          {isSidebarCollapsed ? <PanelLeftOpen className="size-5" /> : <PanelLeftClose className="size-5" />}
        </button>
      </div>

      <div className="hidden justify-center px-4 lg:flex">
        <label className="relative w-full max-w-md">
          <Search className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-[color:var(--cl-tertiary)]" />
          <input
            className="h-10 w-full rounded-lg border border-transparent bg-[color:var(--cl-primary-soft)] pl-9 pr-3 text-sm text-[color:var(--cl-neutral)] placeholder:text-[color:var(--cl-tertiary)] focus:border-[color:var(--cl-line)] focus:bg-white focus:outline-none"
            placeholder="Search experiments or documents..."
            type="search"
          />
        </label>
      </div>

      <div className="ml-auto flex items-center justify-end gap-5">
        <button className="items-center justify-center rounded-md border border-transparent p-2 text-[color:var(--cl-primary)] transition hover:border-[color:var(--cl-line)] hover:bg-[color:var(--cl-primary-soft)] cursor-pointer" type="button">
          <Bell className="size-5" />
        </button>

        <div className="h-8 w-px bg-[color:var(--cl-line)]" />

        <Popover>
          <PopoverTrigger className="group flex items-center gap-3 rounded-lg px-1 py-1 cursor-pointer">
            <div className="hidden text-right leading-tight sm:block">
              <p className="text-sm font-semibold text-[color:var(--cl-primary)]">{userLabel}</p>
            </div>
            <div className="flex size-10 items-center justify-center rounded-full border border-[color:var(--cl-line)] bg-[color:var(--cl-primary-soft)] text-sm font-bold text-[color:var(--cl-primary)] group-hover:border-[color:var(--cl-primary)]">
              {userInitials}
            </div>
          </PopoverTrigger>

          <PopoverContent align="end" className="w-44 rounded-xl border border-[color:var(--cl-line)] bg-white p-1.5">
            <Link
              className="flex h-9 w-full items-center rounded-md px-3 text-left text-sm font-medium text-[color:var(--cl-neutral)] transition hover:bg-[color:var(--cl-primary-soft)]"
              to="/home/profile"
            >
              View Profile
            </Link>

            <hr />

            <button
              className="flex h-9 w-full items-center rounded-md px-3 text-left text-sm font-medium text-red-700 transition hover:bg-red-50 disabled:cursor-not-allowed disabled:opacity-70 cursor-pointer"
              disabled={isLoggingOut}
              onClick={handleLogout}
              type="button"
            >
              {isLoggingOut ? 'Logging out...' : 'Log out'}
            </button>
          </PopoverContent>
        </Popover>
      </div>
    </header>
  )
}
