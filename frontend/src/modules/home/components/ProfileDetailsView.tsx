import { useTranslation } from 'react-i18next';
import { Button } from '@/components/ui/button';
import { getCountryLabelByCode } from '@/lib/countries';
import { type ProfileResponse } from '@/modules/auth/types/profile';
import {
  formatProfileBirth,
  formatProfileGender,
  formatProfileValue,
} from '@/modules/home/utils/profileFormatters';

type ProfileRowProps = Readonly<{
  label: string;
  value: string;
}>;

function ProfileRow({ label, value }: ProfileRowProps) {
  return (
    <div className="rounded-lg border border-border bg-surface-soft px-4 py-3">
      <p className="text-[0.68rem] font-bold tracking-[0.12em] text-muted-foreground uppercase">
        {label}
      </p>
      <p className="mt-1 text-sm font-medium text-foreground">{value}</p>
    </div>
  );
}

type ProfileDetailsViewProps = Readonly<{
  language: string;
  profile: ProfileResponse;
  onEdit: () => void;
}>;

export function ProfileDetailsView({ language, profile, onEdit }: ProfileDetailsViewProps) {
  const { t } = useTranslation();

  return (
    <div>
      <div className="grid gap-3 sm:grid-cols-2">
        <ProfileRow
          label={t('home.profile.firstName')}
          value={formatProfileValue(profile.firstName)}
        />
        <ProfileRow
          label={t('home.profile.lastName')}
          value={formatProfileValue(profile.lastName)}
        />
        <ProfileRow
          label={t('home.profile.birthDate')}
          value={formatProfileBirth(profile.birth, language)}
        />
        <ProfileRow label={t('home.profile.gender')} value={formatProfileGender(profile.gender)} />
        <ProfileRow
          label={t('home.profile.country')}
          value={getCountryLabelByCode(profile.countryCode, language)}
        />
        <ProfileRow label={t('home.profile.city')} value={formatProfileValue(profile.city)} />
      </div>

      <div className="mt-5 flex justify-center">
        <Button
          className="h-10 min-w-32 rounded-md bg-primary text-sm font-semibold text-white hover:bg-primary-strong cursor-pointer"
          onClick={onEdit}
          type="button"
        >
          {t('common.actions.editProfile')}
        </Button>
      </div>
    </div>
  );
}
