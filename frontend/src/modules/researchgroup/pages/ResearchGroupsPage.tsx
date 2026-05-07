import { Mail, Plus } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { PageContainer } from '@/components/common/PageContainer';
import { PageHeader } from '@/components/common/PageHeader';
import { Button } from '@/components/ui/button';
import { CreateResearchGroupDialog } from '@/modules/researchgroup/components/CreateResearchGroupDialog';
import { ResearchGroupsListContent } from '@/modules/researchgroup/components/ResearchGroupsListContent';
import { ResearchGroupInvitationsDialog } from '@/modules/researchgroup/components/ResearchGroupInvitationsDialog';
import { useResearchGroupsPage } from '@/modules/researchgroup/hooks/useResearchGroupsPage';

export default function ResearchGroupsPage() {
  const { t } = useTranslation();
  const { errorMessage, groups, invitations, isLoading, openResearchGroup } =
    useResearchGroupsPage();

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

      <ResearchGroupsListContent
        errorMessage={errorMessage}
        groups={groups}
        isLoading={isLoading}
        onOpenResearchGroup={openResearchGroup}
      />
    </PageContainer>
  );
}
