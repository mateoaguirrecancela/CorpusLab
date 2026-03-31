import { useCallback, useEffect, useMemo, useState } from 'react';
import { CalendarDays, ChevronDown, Flag, Globe, MapPin, UserRound } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Button } from '@/components/ui/button';
import { FeedbackMessage } from '@/components/ui/feedback-message';
import { Spinner } from '@/components/ui/spinner';
import { Input } from '@/components/ui/input';
import { Field, FieldLabel } from '@/components/ui/field';
import { getCountryLabelByCode } from '@/lib/countries';
import { getUserInitials } from '@/lib/user';
import { CountryCombobox } from '@/modules/auth/components/CountryCombobox';
import { getCountryOptions, getGenderOptions } from '@/modules/auth/constants/signup';
import {
  getProfile,
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
  const [profile, setProfile] = useState<ProfileResponse | null>(null);
  const [form, setForm] = useState<ProfileFormState>({
    firstName: '',
    lastName: '',
    birth: '',
    gender: '',
    countryCode: '',
    city: '',
  });
  const [isLoading, setIsLoading] = useState(true);
  const [isEditing, setIsEditing] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
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

  const loadProfile = useCallback(async () => {
    setIsLoading(true);
    setErrorMessage('');

    try {
      const response = await getProfile();
      setProfile(response);
      syncFormWithProfile(response);
    } catch (error) {
      setErrorMessage(getProfileErrorMessage(error));
    } finally {
      setIsLoading(false);
    }
  }, [syncFormWithProfile]);

  const setField = <K extends keyof ProfileFormState>(field: K, value: ProfileFormState[K]) => {
    setForm((current) => ({ ...current, [field]: value }));
  };

  useEffect(() => {
    void loadProfile();
  }, [loadProfile]);

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
    setSuccessMessage('');
    setErrorMessage('');
    setIsEditing(true);
  };

  const handleCancelEditing = () => {
    if (profile) {
      syncFormWithProfile(profile);
    }

    setErrorMessage('');
    setSuccessMessage('');
    setIsEditing(false);
  };

  const handleSaveProfile = async () => {
    if (!canSave) {
      setErrorMessage(t('home.profile.requiredNames'));
      return;
    }

    setIsSaving(true);
    setErrorMessage('');
    setSuccessMessage('');

    try {
      const updatedProfile = await updateProfile({
        firstName: form.firstName,
        lastName: form.lastName,
        birth: form.birth.trim() ? form.birth : undefined,
        gender: form.gender.trim() ? form.gender : undefined,
        countryCode: form.countryCode.trim() ? form.countryCode : undefined,
        city: form.city.trim() ? form.city : undefined,
      });

      setProfile(updatedProfile);
      syncFormWithProfile(updatedProfile);
      setIsEditing(false);
      setSuccessMessage(t('home.profile.updated'));
    } catch (error) {
      setErrorMessage(getUpdateProfileErrorMessage(error));
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

        {!isLoading && errorMessage.length > 0 && (
          <FeedbackMessage
            className="rounded-lg px-4 py-3"
            message={errorMessage}
            variant="error"
          />
        )}

        {!isLoading && (
          <FeedbackMessage
            className="mb-3 rounded-lg px-4 py-3"
            message={successMessage}
            variant="success"
          />
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
                  <Field>
                    <FieldLabel htmlFor="firstName">
                      <UserRound className="size-4" />
                      {t('home.profile.firstName')}
                    </FieldLabel>
                    <Input
                      autoComplete="off"
                      id="firstName"
                      className="bg-white"
                      onChange={(e) => setField('firstName', e.currentTarget.value)}
                      placeholder={t('home.profile.firstNamePlaceholder')}
                      value={form.firstName}
                    />
                  </Field>

                  <Field>
                    <FieldLabel htmlFor="lastName">
                      <UserRound className="size-4" />
                      {t('home.profile.lastName')}
                    </FieldLabel>
                    <Input
                      autoComplete="off"
                      id="lastName"
                      className="bg-white"
                      onChange={(e) => setField('lastName', e.currentTarget.value)}
                      placeholder={t('home.profile.lastNamePlaceholder')}
                      value={form.lastName}
                    />
                  </Field>

                  <Field>
                    <FieldLabel htmlFor="birth">
                      <CalendarDays className="size-4" />
                      {t('home.profile.birthDate')}
                    </FieldLabel>
                    <Input
                      autoComplete="off"
                      id="birth"
                      className="bg-white"
                      onChange={(e) => setField('birth', e.currentTarget.value)}
                      type="date"
                      value={form.birth}
                    />
                  </Field>

                  <Field>
                    <FieldLabel htmlFor="gender">
                      <UserRound className="size-4" />
                      {t('home.profile.gender')}
                    </FieldLabel>
                    <div className="relative">
                      <select
                        className="h-10 w-full appearance-none rounded-md border border-input bg-white px-3 pr-10 text-sm transition-colors outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 disabled:pointer-events-none disabled:cursor-not-allowed disabled:opacity-50"
                        id="gender"
                        onChange={(e) => setField('gender', e.currentTarget.value)}
                        value={form.gender}
                      >
                        {genderOptions.map((option) => (
                          <option key={option.label} value={option.value}>
                            {option.label}
                          </option>
                        ))}
                      </select>
                      <ChevronDown className="pointer-events-none absolute top-1/2 right-3 size-4 -translate-y-1/2 text-[color:var(--cl-tertiary)]" />
                    </div>
                  </Field>

                  <Field>
                    <FieldLabel htmlFor="countryCode">
                      <Globe className="size-4" />
                      {t('home.profile.country')}
                    </FieldLabel>
                    <CountryCombobox
                      id="countryCode"
                      options={countryOptions}
                      placeholder={t('home.profile.searchCountry')}
                      value={form.countryCode}
                      onChange={(value) => setField('countryCode', value)}
                    />
                  </Field>

                  <Field>
                    <FieldLabel htmlFor="city">
                      <MapPin className="size-4" />
                      {t('home.profile.city')}
                    </FieldLabel>
                    <Input
                      autoComplete="off"
                      id="city"
                      className="bg-white"
                      onChange={(e) => setField('city', e.currentTarget.value)}
                      placeholder={t('home.profile.cityPlaceholder')}
                      value={form.city}
                    />
                  </Field>
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
