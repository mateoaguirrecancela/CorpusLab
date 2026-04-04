import { useCallback, useEffect, useMemo, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { CalendarDays, Globe, MapPin, UserRound } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { toast } from 'sonner';
import { FormFieldControl } from '@/components/common/FormFieldControl';
import { Button } from '@/components/ui/button';
import { Spinner } from '@/components/ui/spinner';
import { getCountryLabelByCode } from '@/lib/countries';
import { getUserInitials } from '@/lib/user';
import { CountryCombobox } from '@/modules/auth/components/CountryCombobox';
import { PROFILE_QUERY_KEY, useProfileQuery } from '@/modules/auth/hooks/useProfileQuery';
import { getCountryOptions, getGenderOptions } from '@/modules/auth/constants/signup';
import {
  getProfileErrorMessage,
  getUpdateProfileErrorMessage,
  updateProfile,
} from '@/modules/auth/services/authService';
import { type ProfileFormState, type ProfileResponse } from '@/modules/auth/types/profile';

function formatValue(value: string | null) {
  if (value === null) {
    return '-';
  }

  const trimmed = value.trim();
  return trimmed.length > 0 ? trimmed : '-';
}

function formatGender(value: string | null) {
  if (!value) {
    return '-';
  }

  const normalized = value.toLowerCase();
  return normalized.charAt(0).toUpperCase() + normalized.slice(1);
}

function formatBirth(value: string | null, language: string) {
  if (!value) {
    return '-';
  }

  const parsedDate = new Date(value);
  if (Number.isNaN(parsedDate.getTime())) {
    return value;
  }

  return new Intl.DateTimeFormat(language, {
    day: '2-digit',
    month: 'long',
    year: 'numeric',
  }).format(parsedDate);
}

type ProfileRowProps = {
  label: string;
  value: string;
};

function ProfileRow({ label, value }: Readonly<ProfileRowProps>) {
  return (
    <div className="rounded-lg border border-[color:var(--cl-line)] bg-white/85 px-4 py-3">
      <p className="text-[0.68rem] font-bold tracking-[0.12em] text-[color:var(--cl-secondary)] uppercase">
        {label}
      </p>
      <p className="mt-1 text-sm font-medium text-[color:var(--cl-neutral)]">{value}</p>
    </div>
  );
}

export default function ProfilePage() {
  const { t, i18n } = useTranslation();
  const queryClient = useQueryClient();
  const {
    data: profile,
    error: profileError,
    isError: isProfileError,
    isLoading,
  } = useProfileQuery();
  const [form, setForm] = useState<ProfileFormState>({
    firstName: '',
    lastName: '',
    birth: '',
    gender: '',
    countryCode: '',
    city: '',
  });
  const [isEditing, setIsEditing] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const genderOptions = getGenderOptions(t);
  const countryOptions = useMemo(
    () => getCountryOptions(i18n.resolvedLanguage ?? i18n.language ?? 'en'),
    [i18n.language, i18n.resolvedLanguage],
  );

  const syncFormWithProfile = useCallback((currentProfile: ProfileResponse) => {
    setForm({
      firstName: currentProfile.firstName,
      lastName: currentProfile.lastName,
      birth: currentProfile.birth ?? '',
      gender: currentProfile.gender ?? '',
      countryCode: currentProfile.countryCode ?? '',
      city: currentProfile.city ?? '',
    });
  }, []);

  const setField = <K extends keyof ProfileFormState>(field: K, value: ProfileFormState[K]) => {
    setForm((current) => ({ ...current, [field]: value }));
  };

  useEffect(() => {
    if (profile && !isEditing) {
      syncFormWithProfile(profile);
    }
  }, [isEditing, profile, syncFormWithProfile]);

  useEffect(() => {
    if (isProfileError) {
      toast.error(getProfileErrorMessage(profileError), { id: 'profile-load-error' });
      return;
    }
  }, [isProfileError, profileError]);

  const userInitials = useMemo(() => {
    if (!profile) {
      return t('common.user').charAt(0);
    }

    return getUserInitials(profile);
  }, [profile, t]);

  const canSave = useMemo(() => {
    const hasRequiredNames = form.firstName.trim().length > 0 && form.lastName.trim().length > 0;
    return hasRequiredNames && !isSaving;
  }, [form.firstName, form.lastName, isSaving]);

  const handleStartEditing = () => {
    if (!profile) {
      return;
    }

    syncFormWithProfile(profile);
    setIsEditing(true);
  };

  const handleCancelEditing = () => {
    if (profile) {
      syncFormWithProfile(profile);
    }
    setIsEditing(false);
  };

  const handleSaveProfile = async () => {
    if (!canSave) {
      toast.error(t('home.profile.requiredNames'));
      return;
    }

    setIsSaving(true);

    try {
      const updatedProfile = await updateProfile({
        firstName: form.firstName,
        lastName: form.lastName,
        birth: form.birth.trim() ? form.birth : undefined,
        gender: form.gender.trim() ? form.gender : undefined,
        countryCode: form.countryCode.trim() ? form.countryCode : undefined,
        city: form.city.trim() ? form.city : undefined,
      });

      queryClient.setQueryData(PROFILE_QUERY_KEY, updatedProfile);
      syncFormWithProfile(updatedProfile);
      setIsEditing(false);
      toast.success(t('home.profile.updated'));
    } catch (error) {
      toast.error(getUpdateProfileErrorMessage(error));
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <section className="px-6 py-6 sm:px-8 sm:py-8">
      <h1 className="text-4xl font-black tracking-tight text-[color:var(--cl-primary)]">
        {t('home.profile.title')}
      </h1>

      <div className="mt-6 rounded-2xl border border-[color:var(--cl-line)] bg-[#eef1fb] p-4 sm:p-6">
        {isLoading && (
          <div className="rounded-lg border border-[color:var(--cl-line)] bg-white px-4 py-6 text-sm text-[color:var(--cl-secondary)]">
            <span className="inline-flex items-center gap-2">
              <Spinner aria-hidden className="size-4" />
              {t('common.loading.profile')}
            </span>
          </div>
        )}

        {!isLoading && profile && (
          <div>
            <div className="mb-4 flex items-center gap-4 rounded-xl border border-[color:var(--cl-line)] bg-white px-4 py-3">
              <div className="flex size-16 items-center justify-center rounded-full border border-[color:var(--cl-line)] bg-[color:var(--cl-primary-soft)] text-lg font-bold text-[color:var(--cl-primary)]">
                {userInitials}
              </div>

              <div>
                <p className="text-xs font-bold tracking-[0.1em] text-[color:var(--cl-secondary)] uppercase">
                  {t('common.user')}
                </p>
                <p className="mt-1 text-lg font-semibold text-[color:var(--cl-primary)]">
                  {formatValue(profile.email)}
                </p>
              </div>
            </div>

            {!isEditing && (
              <div>
                <div className="grid gap-3 sm:grid-cols-2">
                  <ProfileRow
                    label={t('home.profile.firstName')}
                    value={formatValue(profile.firstName)}
                  />
                  <ProfileRow
                    label={t('home.profile.lastName')}
                    value={formatValue(profile.lastName)}
                  />
                  <ProfileRow
                    label={t('home.profile.birthDate')}
                    value={formatBirth(profile.birth, i18n.resolvedLanguage ?? 'en')}
                  />
                  <ProfileRow
                    label={t('home.profile.gender')}
                    value={formatGender(profile.gender)}
                  />
                  <ProfileRow
                    label={t('home.profile.country')}
                    value={getCountryLabelByCode(
                      profile.countryCode,
                      i18n.resolvedLanguage ?? i18n.language ?? 'en',
                    )}
                  />
                  <ProfileRow label={t('home.profile.city')} value={formatValue(profile.city)} />
                </div>

                <div className="mt-5 flex justify-center">
                  <Button
                    className="h-10 min-w-32 rounded-md bg-[color:var(--cl-primary)] text-sm font-semibold text-white hover:bg-[color:var(--cl-primary-deep)] cursor-pointer"
                    onClick={handleStartEditing}
                    type="button"
                  >
                    {t('common.actions.editProfile')}
                  </Button>
                </div>
              </div>
            )}

            {isEditing && (
              <div>
                <div className="grid gap-3 sm:grid-cols-2">
                  <FormFieldControl
                    controlClassName="bg-white"
                    icon={<UserRound className="size-4" />}
                    id="firstName"
                    inputProps={{
                      autoComplete: 'off',
                      placeholder: t('home.profile.firstNamePlaceholder'),
                    }}
                    label={t('home.profile.firstName')}
                    onValueChange={(value) => setField('firstName', value)}
                    value={form.firstName}
                  />

                  <FormFieldControl
                    controlClassName="bg-white"
                    icon={<UserRound className="size-4" />}
                    id="lastName"
                    inputProps={{
                      autoComplete: 'off',
                      placeholder: t('home.profile.lastNamePlaceholder'),
                    }}
                    label={t('home.profile.lastName')}
                    onValueChange={(value) => setField('lastName', value)}
                    value={form.lastName}
                  />

                  <FormFieldControl
                    controlClassName="bg-white"
                    icon={<CalendarDays className="size-4" />}
                    id="birth"
                    inputProps={{ autoComplete: 'off' }}
                    inputType="date"
                    label={t('home.profile.birthDate')}
                    onValueChange={(value) => setField('birth', value)}
                    value={form.birth}
                  />

                  <FormFieldControl
                    controlType="select"
                    icon={<UserRound className="size-4" />}
                    id="gender"
                    label={t('home.profile.gender')}
                    onValueChange={(value) => setField('gender', value)}
                    options={genderOptions}
                    value={form.gender}
                  />

                  <FormFieldControl
                    controlType="custom"
                    icon={<Globe className="size-4" />}
                    id="countryCode"
                    label={t('home.profile.country')}
                    renderControl={({ id }) => (
                      <CountryCombobox
                        id={id}
                        options={countryOptions}
                        placeholder={t('home.profile.searchCountry')}
                        value={form.countryCode}
                        onChange={(value) => setField('countryCode', value)}
                      />
                    )}
                  />

                  <FormFieldControl
                    controlClassName="bg-white"
                    icon={<MapPin className="size-4" />}
                    id="city"
                    inputProps={{
                      autoComplete: 'off',
                      placeholder: t('home.profile.cityPlaceholder'),
                    }}
                    label={t('home.profile.city')}
                    onValueChange={(value) => setField('city', value)}
                    value={form.city}
                  />
                </div>

                <div className="mt-5 flex flex-wrap justify-center gap-3">
                  <Button
                    className="h-10 min-w-32 rounded-md border border-[color:var(--cl-line)] bg-white text-sm font-semibold text-[color:var(--cl-neutral)] transition-colors hover:bg-[color:var(--cl-primary-soft)] cursor-pointer"
                    onClick={handleCancelEditing}
                    type="button"
                  >
                    {t('common.actions.cancel')}
                  </Button>
                  <Button
                    className="h-10 min-w-32 rounded-md bg-[color:var(--cl-primary)] text-sm font-semibold text-white transition-colors hover:bg-[color:var(--cl-primary-deep)] disabled:bg-[color:var(--cl-tertiary)] cursor-pointer"
                    disabled={!canSave}
                    onClick={() => void handleSaveProfile()}
                    type="button"
                  >
                    {isSaving ? (
                      <span className="inline-flex items-center gap-2">
                        <Spinner aria-hidden className="size-4" />
                        {t('common.actions.saving')}
                      </span>
                    ) : (
                      t('common.actions.saveChanges')
                    )}
                  </Button>
                </div>
              </div>
            )}
          </div>
        )}
      </div>
    </section>
  );
}
