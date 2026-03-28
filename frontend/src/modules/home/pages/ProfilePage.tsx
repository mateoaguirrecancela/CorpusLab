import { useCallback, useEffect, useMemo, useState } from 'react'
import { CalendarDays, Flag, MapPin, UserRound } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { FeedbackMessage } from '@/components/ui/feedback-message'
import { Spinner } from '@/components/ui/spinner'
import { getCountryLabelByCode } from '@/lib/countries'
import { getUserInitials } from '@/lib/user'
import { AuthCombobox } from '@/modules/auth/components/CountryCombobox'
import { AuthFormField } from '@/modules/auth/components/AuthFormField'
import { AuthSelectField } from '@/modules/auth/components/AuthSelectField'
import { COUNTRY_OPTIONS, GENDER_OPTIONS } from '@/modules/auth/constants/signup'
import {
  getProfile,
  getProfileErrorMessage,
  getUpdateProfileErrorMessage,
  updateProfile,
} from '@/modules/auth/services/authService'
import { type ProfileFormState, type ProfileResponse } from '@/modules/auth/types/profile'

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
  const [form, setForm] = useState<ProfileFormState>({
    firstName: '',
    lastName: '',
    birth: '',
    gender: '',
    countryCode: '',
    city: '',
  })
  const [isLoading, setIsLoading] = useState(true)
  const [isEditing, setIsEditing] = useState(false)
  const [isSaving, setIsSaving] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')
  const [successMessage, setSuccessMessage] = useState('')

  const syncFormWithProfile = useCallback((currentProfile: ProfileResponse) => {
    setForm({
      firstName: currentProfile.firstName,
      lastName: currentProfile.lastName,
      birth: currentProfile.birth ?? '',
      gender: currentProfile.gender ?? '',
      countryCode: currentProfile.countryCode ?? '',
      city: currentProfile.city ?? '',
    })
  }, [])

  const loadProfile = useCallback(async () => {
    setIsLoading(true)
    setErrorMessage('')

    try {
      const response = await getProfile()
      setProfile(response)
      syncFormWithProfile(response)
    } catch (error) {
      setErrorMessage(getProfileErrorMessage(error))
    } finally {
      setIsLoading(false)
    }
  }, [syncFormWithProfile])

  const setField = <K extends keyof ProfileFormState>(field: K, value: ProfileFormState[K]) => {
    setForm((current) => ({ ...current, [field]: value }))
  }

  useEffect(() => {
    void loadProfile()
  }, [loadProfile])

  const userInitials = useMemo(() => {
    if (!profile) {
      return 'U'
    }

    return getUserInitials(profile)
  }, [profile])

  const canSave = useMemo(() => {
    const hasRequiredNames = form.firstName.trim().length > 0 && form.lastName.trim().length > 0
    return hasRequiredNames && !isSaving
  }, [form.firstName, form.lastName, isSaving])

  const handleStartEditing = () => {
    if (!profile) {
      return
    }

    syncFormWithProfile(profile)
    setSuccessMessage('')
    setErrorMessage('')
    setIsEditing(true)
  }

  const handleCancelEditing = () => {
    if (profile) {
      syncFormWithProfile(profile)
    }

    setErrorMessage('')
    setSuccessMessage('')
    setIsEditing(false)
  }

  const handleSaveProfile = async () => {
    if (!canSave) {
      setErrorMessage('Please complete first name and last name before saving.')
      return
    }

    setIsSaving(true)
    setErrorMessage('')
    setSuccessMessage('')

    try {
      const updatedProfile = await updateProfile({
        firstName: form.firstName,
        lastName: form.lastName,
        birth: form.birth.trim() ? form.birth : undefined,
        gender: form.gender.trim() ? form.gender : undefined,
        countryCode: form.countryCode.trim() ? form.countryCode : undefined,
        city: form.city.trim() ? form.city : undefined,
      })

      setProfile(updatedProfile)
      syncFormWithProfile(updatedProfile)
      setIsEditing(false)
      setSuccessMessage('Profile updated successfully.')
    } catch (error) {
      setErrorMessage(getUpdateProfileErrorMessage(error))
    } finally {
      setIsSaving(false)
    }
  }

  return (
    <section className="px-6 py-6 sm:px-8 sm:py-8">
      <h1 className="text-4xl font-black tracking-tight text-[color:var(--cl-primary)]">My Profile</h1>

      <div className="mt-6 rounded-2xl border border-[color:var(--cl-line)] bg-[#eef1fb] p-4 sm:p-6">
        {isLoading && (
          <div className="rounded-lg border border-[color:var(--cl-line)] bg-white px-4 py-6 text-sm text-[color:var(--cl-secondary)]">
            <span className="inline-flex items-center gap-2">
              <Spinner aria-hidden className="size-4" />
              Loading profile...
            </span>
          </div>
        )}

        {!isLoading && errorMessage.length > 0 && <FeedbackMessage className="rounded-lg px-4 py-3" message={errorMessage} variant="error" />}

        {!isLoading && <FeedbackMessage className="mb-3 rounded-lg px-4 py-3" message={successMessage} variant="success" />}

        {!isLoading && profile && (
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

            {!isEditing && (
              <div>
                <div className="grid gap-3 sm:grid-cols-2">
                  <ProfileRow label="First Name" value={formatValue(profile.firstName)} />
                  <ProfileRow label="Last Name" value={formatValue(profile.lastName)} />
                  <ProfileRow label="Birth Date" value={formatBirth(profile.birth)} />
                  <ProfileRow label="Gender" value={formatGender(profile.gender)} />
                  <ProfileRow label="Country" value={getCountryLabelByCode(profile.countryCode)} />
                  <ProfileRow label="City" value={formatValue(profile.city)} />
                </div>

                <div className="mt-5 flex justify-center">
                  <Button className="h-10 min-w-32 rounded-md bg-[color:var(--cl-primary)] text-sm font-semibold text-white hover:bg-[color:var(--cl-primary-deep)] cursor-pointer" onClick={handleStartEditing} type="button">
                    Edit profile
                  </Button>
                </div>
              </div>
            )}

            {isEditing && (
              <div>
                <div className="grid gap-3 sm:grid-cols-2">
                  <AuthFormField
                    icon={<UserRound className="size-3" />}
                    id="firstName"
                    label="First Name"
                    onChange={(value) => setField('firstName', value)}
                    placeholder="Your first name"
                    value={form.firstName}
                  />

                  <AuthFormField
                    icon={<UserRound className="size-3" />}
                    id="lastName"
                    label="Last Name"
                    onChange={(value) => setField('lastName', value)}
                    placeholder="Your last name"
                    value={form.lastName}
                  />

                  <AuthFormField
                    icon={<CalendarDays className="size-3" />}
                    id="birth"
                    label="Birth Date"
                    onChange={(value) => setField('birth', value)}
                    type="date"
                    value={form.birth}
                  />

                  <AuthSelectField
                    icon={<UserRound className="size-3" />}
                    id="gender"
                    label="Gender"
                    onChange={(value) => setField('gender', value)}
                    options={GENDER_OPTIONS}
                    value={form.gender}
                  />

                  <AuthCombobox
                    icon={<Flag className="size-3" />}
                    id="countryCode"
                    label="Country"
                    onChange={(value) => setField('countryCode', value)}
                    options={COUNTRY_OPTIONS}
                    placeholder="Search country"
                    value={form.countryCode}
                  />

                  <AuthFormField
                    icon={<MapPin className="size-3" />}
                    id="city"
                    label="City"
                    onChange={(value) => setField('city', value)}
                    placeholder="Your city"
                    value={form.city}
                  />
                </div>

                <div className="mt-5 flex flex-wrap justify-center gap-3">
                  <Button className="h-10 min-w-32 rounded-md border border-[color:var(--cl-line)] bg-white text-sm font-semibold text-[color:var(--cl-neutral)] transition-colors hover:bg-[color:var(--cl-primary-soft)] cursor-pointer" onClick={handleCancelEditing} type="button">
                    Cancel
                  </Button>
                  <Button className="h-10 min-w-32 rounded-md bg-[color:var(--cl-primary)] text-sm font-semibold text-white transition-colors hover:bg-[color:var(--cl-primary-deep)] disabled:bg-[color:var(--cl-tertiary)] cursor-pointer" disabled={!canSave} onClick={() => void handleSaveProfile()} type="button">
                    {isSaving ? (
                      <span className="inline-flex items-center gap-2">
                        <Spinner aria-hidden className="size-4" />
                        Saving...
                      </span>
                    ) : (
                      'Save changes'
                    )}
                  </Button>
                </div>
              </div>
            )}
          </div>
        )}
      </div>
    </section>
  )
}
