import { useTranslation } from 'react-i18next';
import { Button } from '@/components/ui/button';
import { getCountryLabelByCode } from '@/shared/constants/countries';
import { getResolvedLanguage } from '@/app/config/i18n';
import { type ProfileResponse } from '@/modules/auth/types/profile';
import {
  formatProfileBirth,
  formatProfileGender,
  formatProfileValue,
} from '@/modules/auth/profile/utils/profileFormatters';

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
  profile: ProfileResponse;
  onEdit: () => void;
}>;

export function ProfileDetailsView({ profile, onEdit }: ProfileDetailsViewProps) {
  const { i18n, t } = useTranslation();
  const language = getResolvedLanguage(i18n);

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
        <ProfileRow
          label={t('home.profile.gender')}
          value={formatProfileGender(profile.gender, t)}
        />
        <ProfileRow
          label={t('home.profile.country')}
          value={getCountryLabelByCode(profile.countryCode, language)}
        />
        <ProfileRow label={t('home.profile.city')} value={formatProfileValue(profile.city)} />
      </div>

      <div className="mt-5 flex justify-center">
        <Button onClick={onEdit} size="action-md" type="button" variant="primaryAction">
          {t('common.actions.editProfile')}
        </Button>
      </div>
    </div>
  );
}
