import { CalendarDays, Globe, MapPin, UserRound } from 'lucide-react';
import { useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { FormFieldControl } from '@/components/common/FormFieldControl';
import { Button } from '@/components/ui/button';
import { Spinner } from '@/components/ui/spinner';
import { CountryCombobox } from '@/modules/auth/components/CountryCombobox';
import { getCountryOptions, getGenderOptions } from '@/modules/auth/constants/signup';
import { type ProfileFormState } from '@/modules/auth/types/profile';

type ProfileEditFormProps = Readonly<{
  canSave: boolean;
  form: ProfileFormState;
  isSaving: boolean;
  language: string;
  onCancel: () => void;
  onSave: () => void;
  onFieldChange: <K extends keyof ProfileFormState>(field: K, value: ProfileFormState[K]) => void;
}>;

export function ProfileEditForm({
  canSave,
  form,
  isSaving,
  language,
  onCancel,
  onSave,
  onFieldChange,
}: ProfileEditFormProps) {
  const { t } = useTranslation();
  const genderOptions = getGenderOptions(t);
  const countryOptions = useMemo(() => getCountryOptions(language), [language]);

  return (
    <div>
      <div className="grid gap-3 sm:grid-cols-2">
        <FormFieldControl
          controlClassName="bg-surface-base"
          icon={<UserRound className="size-4" />}
          id="firstName"
          inputProps={{
            autoComplete: 'off',
            placeholder: t('home.profile.firstNamePlaceholder'),
          }}
          label={t('home.profile.firstName')}
          onValueChange={(value) => onFieldChange('firstName', value)}
          value={form.firstName}
        />

        <FormFieldControl
          controlClassName="bg-surface-base"
          icon={<UserRound className="size-4" />}
          id="lastName"
          inputProps={{
            autoComplete: 'off',
            placeholder: t('home.profile.lastNamePlaceholder'),
          }}
          label={t('home.profile.lastName')}
          onValueChange={(value) => onFieldChange('lastName', value)}
          value={form.lastName}
        />

        <FormFieldControl
          controlClassName="bg-surface-base"
          icon={<CalendarDays className="size-4" />}
          id="birth"
          inputProps={{ autoComplete: 'off' }}
          inputType="date"
          label={t('home.profile.birthDate')}
          onValueChange={(value) => onFieldChange('birth', value)}
          value={form.birth}
        />

        <FormFieldControl
          controlType="select"
          icon={<UserRound className="size-4" />}
          id="gender"
          label={t('home.profile.gender')}
          onValueChange={(value) => onFieldChange('gender', value)}
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
              onChange={(value) => onFieldChange('countryCode', value)}
            />
          )}
        />

        <FormFieldControl
          controlClassName="bg-surface-base"
          icon={<MapPin className="size-4" />}
          id="city"
          inputProps={{
            autoComplete: 'off',
            placeholder: t('home.profile.cityPlaceholder'),
          }}
          label={t('home.profile.city')}
          onValueChange={(value) => onFieldChange('city', value)}
          value={form.city}
        />
      </div>

      <div className="mt-5 flex flex-wrap justify-center gap-3">
        <Button
          className="h-10 min-w-32 rounded-md border border-border bg-surface-base text-sm font-semibold text-foreground transition-colors hover:bg-accent cursor-pointer"
          onClick={onCancel}
          type="button"
        >
          {t('common.actions.cancel')}
        </Button>
        <Button
          className="h-10 min-w-32 rounded-md bg-primary text-sm font-semibold text-white transition-colors hover:bg-primary-strong disabled:bg-muted cursor-pointer"
          disabled={!canSave}
          onClick={onSave}
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
  );
}
