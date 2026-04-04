import { useEffect } from 'react';
import { Mail, Plus } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { toast } from 'sonner';
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

  return (
    <section className="px-6 py-6 sm:px-8 sm:py-8">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <h1 className="text-4xl font-black tracking-tight text-[color:var(--cl-primary)]">
          {t('researchGroup.title')}
        </h1>

        <div className="flex gap-3">
          <ResearchGroupInvitationsDialog
            trigger={
              <Button
                className="h-10 px-4 rounded-md border border-[color:var(--cl-line)] bg-white text-sm font-semibold text-[color:var(--cl-neutral)] transition-colors hover:bg-[color:var(--cl-primary-soft)] cursor-pointer"
                type="button"
              >
                <Mail className="size-4" />
                {t('researchGroup.invitations')}
                {invitations.length > 0 && (
                  <span className="inline-flex size-5 items-center justify-center rounded-full bg-(--cl-neutral) text-[11px] font-bold leading-none text-white">
                    {invitations.length}
                  </span>
                )}
              </Button>
            }
          />

          <CreateResearchGroupDialog
            trigger={
              <Button
                className="h-10 px-4 rounded-md bg-[color:var(--cl-primary)] text-sm font-semibold text-white transition-colors hover:bg-[color:var(--cl-primary-deep)] disabled:bg-[color:var(--cl-tertiary)] cursor-pointer"
                type="button"
              >
                <Plus className="size-4" />
                {t('researchGroup.newGroup')}
              </Button>
            }
          />
        </div>
      </div>

      <div className="mt-6">
        {isLoading && (
          <div className="rounded-lg border border-[color:var(--cl-line)] bg-white px-4 py-6 text-sm text-[color:var(--cl-secondary)]">
            <span className="inline-flex items-center gap-2">
              <Spinner aria-hidden className="size-4" />
              {t('researchGroup.loading')}
            </span>
          </div>
        )}

        {!isLoading && errorMessage.length === 0 && groups.length === 0 && (
          <div className="rounded-lg border border-[color:var(--cl-line)] bg-white px-4 py-6 text-center text-sm text-[color:var(--cl-secondary)]">
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
    </section>
  );
}
