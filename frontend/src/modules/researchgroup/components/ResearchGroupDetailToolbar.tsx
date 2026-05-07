import { FilePenLine } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { BackButton } from '@/components/common/BackButton';
import { Button } from '@/components/ui/button';
import { EditResearchGroupDialog } from '@/modules/researchgroup/components/EditResearchGroupDialog';
import { type ResearchGroupDetail } from '@/modules/researchgroup/types/researchGroup';

type ResearchGroupDetailToolbarProps = Readonly<{
  canManageResearchers: boolean;
  group: ResearchGroupDetail;
  onDeleted: () => void;
}>;

export function ResearchGroupDetailToolbar({
  canManageResearchers,
  group,
  onDeleted,
}: ResearchGroupDetailToolbarProps) {
  const { t } = useTranslation();

  return (
    <div className="flex items-center justify-between gap-3">
      <BackButton fallbackTo="/home/research-groups" />

      {canManageResearchers && (
        <EditResearchGroupDialog
          groupId={group.id}
          initialDescription={group.description}
          initialName={group.name}
          onDeleted={onDeleted}
          showDeleteButton
          trigger={
            <Button
              className="h-10 rounded-md border border-border bg-surface-base px-4 text-sm font-semibold text-primary hover:bg-accent cursor-pointer"
              type="button"
              variant="outline"
            >
              <FilePenLine className="size-4" />
              {t('researchGroup.detail.editGroup')}
            </Button>
          }
        />
      )}
    </div>
  );
}
