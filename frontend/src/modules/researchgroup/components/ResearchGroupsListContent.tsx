import { useTranslation } from 'react-i18next';
import { EntitySummaryCard } from '@/components/common/EntitySummaryCard';
import { Spinner } from '@/components/ui/spinner';
import { type ResearchGroupSummary } from '@/modules/researchgroup/types/researchGroup';

type ResearchGroupsListContentProps = Readonly<{
  errorMessage: string;
  groups: ResearchGroupSummary[];
  isLoading: boolean;
  onOpenResearchGroup: (groupId: number) => void;
}>;

export function ResearchGroupsListContent({
  errorMessage,
  groups,
  isLoading,
  onOpenResearchGroup,
}: ResearchGroupsListContentProps) {
  const { t } = useTranslation();

  return (
    <div className="mt-6">
      {isLoading && (
        <div className="text-sm text-muted-foreground">
          <span className="inline-flex items-center gap-2">
            <Spinner aria-hidden className="size-4" />
            {t('researchGroup.loading')}
          </span>
        </div>
      )}

      {!isLoading && errorMessage.length === 0 && groups.length === 0 && (
        <div className="text-sm text-muted-foreground">{t('researchGroup.noGroups')}</div>
      )}

      {!isLoading && groups.length > 0 && (
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
          {groups.map((group) => {
            const roleLabel = t(`researchGroup.roles.${group.role}`);

            return (
              <EntitySummaryCard
                actionLabel={t('researchGroup.enter')}
                description={group.description}
                footerMeta={{
                  kind: 'members',
                  text: t('researchGroup.memberCount', { count: group.memberCount }),
                }}
                key={group.id}
                onAction={() => onOpenResearchGroup(group.id)}
                roleLabel={roleLabel}
                role={group.role}
                title={group.name}
              />
            );
          })}
        </div>
      )}
    </div>
  );
}
