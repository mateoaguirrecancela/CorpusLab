import { useEffect } from 'react';
import { Mail, Plus } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { toast } from 'sonner';
import { PageContainer } from '@/components/common/PageContainer';
import { PageHeader } from '@/components/common/PageHeader';
import { Spinner } from '@/components/ui/spinner';
import { Button } from '@/components/ui/button';
import { CreateResearchGroupDialog } from '@/modules/researchgroup/components/CreateResearchGroupDialog';
import { ResearchGroupCard } from '@/modules/researchgroup/components/ResearchGroupCard';
import { ResearchGroupInvitationsDialog } from '@/modules/researchgroup/components/ResearchGroupInvitationsDialog';
import { getResearchGroupsErrorMessage } from '@/modules/researchgroup/services/researchGroupService';
import {
  useResearchGroupInvitationsQuery,
  useResearchGroupsQuery,
} from '@/modules/researchgroup/hooks/useResearchGroupQueries';

export default function ResearchGroupsPage() {
  const { t } = useTranslation();
  const { data: groups = [], isLoading, isError, error } = useResearchGroupsQuery();
  const { data: invitations = [] } = useResearchGroupInvitationsQuery();
  const errorMessage = isError ? getResearchGroupsErrorMessage(error) : '';

  useEffect(() => {
    if (errorMessage.length > 0) {
      toast.error(errorMessage, { id: 'research-groups-load-error' });
    }
  }, [errorMessage]);

  const actions = (
    <>
      <ResearchGroupInvitationsDialog
        trigger={
          <Button
            className="h-10 px-4 rounded-md border border-border bg-surface-base text-sm font-semibold text-foreground transition-colors hover:bg-accent cursor-pointer"
            type="button"
          >
            <Mail className="size-4" />
            {t('researchGroup.invitations')}
            {invitations.length > 0 && (
              <span className="inline-flex size-5 items-center justify-center rounded-full bg-foreground text-[11px] font-bold leading-none text-white">
                {invitations.length}
              </span>
            )}
          </Button>
        }
      />

      <CreateResearchGroupDialog
        trigger={
          <Button
            className="h-10 px-4 rounded-md bg-primary text-sm font-semibold text-white transition-colors hover:bg-primary-strong disabled:bg-muted cursor-pointer"
            type="button"
          >
            <Plus className="size-4" />
            {t('researchGroup.newGroup')}
          </Button>
        }
      />
    </>
  );

  return (
    <PageContainer>
      <PageHeader actions={actions} title={t('researchGroup.title')} />

      <div className="mt-6">
        {isLoading && (
          <div className="rounded-lg border border-border bg-surface-base px-4 py-6 text-sm text-muted-foreground">
            <span className="inline-flex items-center gap-2">
              <Spinner aria-hidden className="size-4" />
              {t('researchGroup.loading')}
            </span>
          </div>
        )}

        {!isLoading && errorMessage.length === 0 && groups.length === 0 && (
          <div className="rounded-lg border border-border bg-surface-base px-4 py-6 text-center text-sm text-muted-foreground">
            {t('researchGroup.noGroups')}
          </div>
        )}

        {!isLoading && groups.length > 0 && (
          <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
            {groups.map((group) => (
              <ResearchGroupCard group={group} key={group.id} />
            ))}
          </div>
        )}
      </div>
    </PageContainer>
  );
}
