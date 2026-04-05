import { ArrowRight, Users } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router';
import {
  type ResearchGroupSummary,
  type ResearchGroupMemberRole,
} from '@/modules/researchgroup/types/researchGroup';

const ROLE_BADGE_STYLES: Record<ResearchGroupMemberRole, string> = {
  OWNER: 'bg-primary text-white',
  ADMIN: 'bg-primary text-white',
  ANNOTATOR: 'bg-accent text-primary',
};

type ResearchGroupCardProps = {
  group: ResearchGroupSummary;
};

export function ResearchGroupCard({ group }: Readonly<ResearchGroupCardProps>) {
  const { t } = useTranslation();
  const navigate = useNavigate();

  return (
    <div className="stagger flex flex-col justify-between rounded-2xl border border-border bg-surface-base p-5 transition-shadow hover:shadow-[var(--shadow-card-hover)]">
      <div>
        <div className="flex items-start justify-between gap-2">
          <h3 className="text-base font-bold text-primary">{group.name}</h3>
          <span
            className={[
              'rounded-md px-2.5 py-1 text-xs font-bold tracking-wider uppercase',
              ROLE_BADGE_STYLES[group.role],
            ].join(' ')}
          >
            {t(`researchGroup.roles.${group.role}`)}
          </span>
        </div>

        {group.description && (
          <p className="mt-4 line-clamp-2 text-sm leading-relaxed text-muted-foreground">
            {group.description}
          </p>
        )}
      </div>

      <div className="mt-5 flex items-center justify-between border-t border-border pt-4">
        <span className="inline-flex items-center gap-1.5 text-xs font-medium text-muted-foreground">
          <Users className="size-3.5" />
          {t('researchGroup.memberCount', { count: group.memberCount })}
        </span>

        <button
          className="group inline-flex cursor-pointer items-center gap-1 text-sm font-semibold text-primary transition-colors hover:text-primary-strong"
          onClick={() => navigate(`/home/research-groups/${group.id}`)}
          type="button"
        >
          {t('researchGroup.enter')}
          <ArrowRight className="size-3.5 transition-transform group-hover:translate-x-0.5" />
        </button>
      </div>
    </div>
  );
}
