import { useEffect, useMemo, useState } from 'react'
import { Button } from '@/components/ui/button'
import { getCountryLabelByCode } from '@/lib/countries'
import { getUserInitials } from '@/lib/user'
import { getProfile, getProfileErrorMessage } from '@/modules/auth/services/authService'
import { type ProfileResponse } from '@/modules/auth/types/profile'

function formatValue(value: string | null) {
  if (value === null) {
    return '-'
  }

  const trimmed = value.trim()
  return trimmed.length > 0 ? trimmed : '-'
}

function formatGender(value: string | null) {
  if (!value) {
    return '-'
  }

  const normalized = value.toLowerCase()
  return normalized.charAt(0).toUpperCase() + normalized.slice(1)
}

function formatBirth(value: string | null) {
  if (!value) {
    return '-'
  }

  const parsedDate = new Date(value)
  if (Number.isNaN(parsedDate.getTime())) {
    return value
  }

  return new Intl.DateTimeFormat('es-ES', {
    day: '2-digit',
    month: 'long',
    year: 'numeric',
  }).format(parsedDate)
}

type ProfileRowProps = {
  label: string
  value: string
}

function ProfileRow({ label, value }: ProfileRowProps) {
  return (
    <div className="rounded-lg border border-[color:var(--cl-line)] bg-white/85 px-4 py-3">
      <p className="text-[0.68rem] font-bold tracking-[0.12em] text-[color:var(--cl-secondary)] uppercase">{label}</p>
      <p className="mt-1 text-sm font-medium text-[color:var(--cl-neutral)]">{value}</p>
    </div>
  )
}

export default function ProfilePage() {
  const [profile, setProfile] = useState<ProfileResponse | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [errorMessage, setErrorMessage] = useState('')

  const loadProfile = async () => {
    setIsLoading(true)
    setErrorMessage('')

    try {
      const response = await getProfile()
      setProfile(response)
    } catch (error) {
      setErrorMessage(getProfileErrorMessage(error))
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => {
    void loadProfile()
  }, [])

  const userInitials = useMemo(() => {
    if (!profile) {
      return 'U'
    }

    return getUserInitials(profile)
  }, [profile])

  return (
    <section className="px-6 py-6 sm:px-8 sm:py-8">
      <h1 className="text-4xl font-black tracking-tight text-[color:var(--cl-primary)]">My Profile</h1>

      <div className="mt-6 rounded-2xl border border-[color:var(--cl-line)] bg-[#eef1fb] p-4 sm:p-6">
        {isLoading && (
          <div className="rounded-lg border border-[color:var(--cl-line)] bg-white px-4 py-6 text-sm text-[color:var(--cl-secondary)]">
            Loading profile...
          </div>
        )}

        {!isLoading && errorMessage.length > 0 && (
          <div className="space-y-3">
            <div className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{errorMessage}</div>
            <Button className="cursor-pointer" onClick={() => void loadProfile()} type="button" variant="outline">
              Retry
            </Button>
          </div>
        )}

        {!isLoading && !errorMessage && profile && (
          <div>
            <div className="mb-4 flex items-center gap-4 rounded-xl border border-[color:var(--cl-line)] bg-white px-4 py-3">
              <div className="flex size-16 items-center justify-center rounded-full border border-[color:var(--cl-line)] bg-[color:var(--cl-primary-soft)] text-lg font-bold text-[color:var(--cl-primary)]">
                {userInitials}
              </div>

              <div>
                <p className="text-xs font-bold tracking-[0.1em] text-[color:var(--cl-secondary)] uppercase">User</p>
                <p className="mt-1 text-lg font-semibold text-[color:var(--cl-primary)]">{formatValue(profile.email)}</p>
              </div>
            </div>

            <div className="grid gap-3 sm:grid-cols-2">
              <ProfileRow label="First Name" value={formatValue(profile.firstName)} />
              <ProfileRow label="Last Name" value={formatValue(profile.lastName)} />
              <ProfileRow label="Birth Date" value={formatBirth(profile.birth)} />
              <ProfileRow label="Gender" value={formatGender(profile.gender)} />
              <ProfileRow label="Country" value={getCountryLabelByCode(profile.countryCode)} />
              <ProfileRow label="City" value={formatValue(profile.city)} />
            </div>
          </div>
        )}
      </div>
    </section>
  )
}
