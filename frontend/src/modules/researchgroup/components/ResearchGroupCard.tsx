import { ArrowRight, Users } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router';
import {
  type ResearchGroupSummary,
  type ResearchGroupMemberRole,
} from '@/modules/researchgroup/types/researchGroup';

const ROLE_BADGE_STYLES: Record<ResearchGroupMemberRole, string> = {
  OWNER: 'bg-[color:var(--cl-primary)] text-white',
  ADMIN: 'bg-[color:var(--cl-primary)] text-white',
  ANNOTATOR: 'bg-[color:var(--cl-primary-soft)] text-[color:var(--cl-primary)]',
};

type ResearchGroupCardProps = {
  group: ResearchGroupSummary;
};

export function ResearchGroupCard({ group }: Readonly<ResearchGroupCardProps>) {
  const { t } = useTranslation();
  const navigate = useNavigate();

  return (
    <div className="stagger flex flex-col justify-between rounded-2xl border border-[color:var(--cl-line)] bg-white p-5 transition-shadow hover:shadow-md">
      <div>
        <div className="flex items-start justify-between gap-2">
          <h3 className="text-base font-bold text-[color:var(--cl-primary)]">{group.name}</h3>
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
          <p className="mt-4 line-clamp-2 text-sm leading-relaxed text-[color:var(--cl-secondary)]">
            {group.description}
          </p>
        )}
      </div>

      <div className="mt-5 flex items-center justify-between border-t border-[color:var(--cl-line)] pt-4">
        <span className="inline-flex items-center gap-1.5 text-xs font-medium text-[color:var(--cl-secondary)]">
          <Users className="size-3.5" />
          {t('researchGroup.memberCount', { count: group.memberCount })}
        </span>

        <button
          className="group inline-flex cursor-pointer items-center gap-1 text-sm font-semibold text-[color:var(--cl-primary)] transition-colors hover:text-[color:var(--cl-primary-deep)]"
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
