import { useTranslation } from 'react-i18next';
import { type ProfileResponse } from '@/modules/auth/types/profile';
import { formatProfileValue } from '@/modules/auth/profile/utils/profileFormatters';

type ProfileUserCardProps = Readonly<{
  profile: ProfileResponse;
  userInitials: string;
}>;

export function ProfileUserCard({ profile, userInitials }: ProfileUserCardProps) {
  const { t } = useTranslation();

  return (
    <div className="mb-4 flex items-center gap-4 rounded-xl border border-border bg-surface-base px-4 py-3">
      <div className="flex size-16 items-center justify-center rounded-full border border-border bg-accent text-lg font-bold text-primary">
        {userInitials}
      </div>

      <div>
        <p className="text-xs font-bold tracking-[0.1em] text-muted-foreground uppercase">
          {t('common.user')}
        </p>
        <p className="mt-1 text-lg font-semibold text-primary">
          {formatProfileValue(profile.email)}
        </p>
      </div>
    </div>
  );
}
