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
          <Button size="action" type="button" variant="secondaryAction">
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
          <Button size="action" type="button" variant="primaryAction">
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
