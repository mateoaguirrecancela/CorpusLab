import { CalendarDays, Globe, MapPin, UserRound } from 'lucide-react';
import { type ComponentProps, type ReactNode, useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { FormFieldControl } from '@/components/common/FormFieldControl';
import { Button } from '@/components/ui/button';
import { Spinner } from '@/components/ui/spinner';
import { buildCountryOptions } from '@/shared/constants/countries';
import { getResolvedLanguage } from '@/app/config/i18n';
import { CountryCombobox } from '@/modules/auth/components/CountryCombobox';
import { type ProfileFormState } from '@/modules/auth/types/profile';
import { getProfileGenderOptions } from '@/modules/auth/profile/utils/profileOptions';

type ProfileFieldChangeHandler = <K extends keyof ProfileFormState>(
  field: K,
  value: ProfileFormState[K],
) => void;

type ProfileEditFormProps = Readonly<{
  canSave: boolean;
  fieldErrors?: Partial<Record<keyof ProfileFormState, string>>;
  form: ProfileFormState;
  isSaving: boolean;
  onCancel: () => void;
  onSave: () => void;
  onFieldChange: ProfileFieldChangeHandler;
}>;

type ProfileInputFieldProps = Readonly<{
  field: keyof ProfileFormState;
  form: ProfileFormState;
  icon: ReactNode;
  label: string;
  message?: string;
  inputType?: ComponentProps<'input'>['type'];
  placeholder?: string;
  onFieldChange: ProfileFieldChangeHandler;
}>;

type ProfileEditActionsProps = Readonly<{
  canSave: boolean;
  isSaving: boolean;
  onCancel: () => void;
  onSave: () => void;
}>;

function ProfileInputField({
  field,
  form,
  icon,
  inputType,
  label,
  message,
  placeholder,
  onFieldChange,
}: ProfileInputFieldProps) {
  return (
    <FormFieldControl
      controlClassName="bg-surface-base"
      icon={icon}
      id={field}
      inputProps={{
        autoComplete: 'off',
        placeholder,
      }}
      inputType={inputType}
      label={label}
      message={message}
      onValueChange={(value) => onFieldChange(field, value)}
      required
      value={form[field]}
    />
  );
}

function ProfileEditActions({ canSave, isSaving, onCancel, onSave }: ProfileEditActionsProps) {
  const { t } = useTranslation();

  return (
    <div className="mt-5 flex flex-wrap justify-center gap-3">
      <Button onClick={onCancel} size="action-md" type="button" variant="secondaryAction">
        {t('common.actions.cancel')}
      </Button>
      <Button
        disabled={!canSave}
        onClick={onSave}
        size="action-md"
        type="button"
        variant="primaryAction"
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
  );
}

export function ProfileEditForm({
  canSave,
  fieldErrors = {},
  form,
  isSaving,
  onCancel,
  onSave,
  onFieldChange,
}: ProfileEditFormProps) {
  const { i18n, t } = useTranslation();
  const language = getResolvedLanguage(i18n);
  const genderOptions = getProfileGenderOptions(t);
  const countryOptions = useMemo(() => buildCountryOptions(language), [language]);

  return (
    <div>
      <div className="grid gap-3 sm:grid-cols-2">
        <ProfileInputField
          field="firstName"
          form={form}
          icon={<UserRound className="size-4" />}
          label={t('home.profile.firstName')}
          message={fieldErrors.firstName}
          placeholder={t('home.profile.firstNamePlaceholder')}
          onFieldChange={onFieldChange}
        />

        <ProfileInputField
          field="lastName"
          form={form}
          icon={<UserRound className="size-4" />}
          label={t('home.profile.lastName')}
          message={fieldErrors.lastName}
          placeholder={t('home.profile.lastNamePlaceholder')}
          onFieldChange={onFieldChange}
        />

        <ProfileInputField
          field="birth"
          form={form}
          icon={<CalendarDays className="size-4" />}
          inputType="date"
          label={t('home.profile.birthDate')}
          message={fieldErrors.birth}
          onFieldChange={onFieldChange}
        />

        <FormFieldControl
          controlType="select"
          icon={<UserRound className="size-4" />}
          id="gender"
          label={t('home.profile.gender')}
          message={fieldErrors.gender}
          onValueChange={(value) => onFieldChange('gender', value)}
          options={genderOptions}
          required
          value={form.gender}
        />

        <FormFieldControl
          controlType="custom"
          icon={<Globe className="size-4" />}
          id="countryCode"
          label={t('home.profile.country')}
          message={fieldErrors.countryCode}
          renderControl={({ id }) => (
            <CountryCombobox
              id={id}
              options={countryOptions}
              placeholder={t('home.profile.searchCountry')}
              value={form.countryCode}
              onChange={(value) => onFieldChange('countryCode', value)}
            />
          )}
          required
        />

        <ProfileInputField
          field="city"
          form={form}
          icon={<MapPin className="size-4" />}
          label={t('home.profile.city')}
          message={fieldErrors.city}
          placeholder={t('home.profile.cityPlaceholder')}
          onFieldChange={onFieldChange}
        />
      </div>

      <ProfileEditActions
        canSave={canSave}
        isSaving={isSaving}
        onCancel={onCancel}
        onSave={onSave}
      />
    </div>
  );
}
