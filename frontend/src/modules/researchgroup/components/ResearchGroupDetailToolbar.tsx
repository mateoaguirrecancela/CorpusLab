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
            <Button size="action" type="button" variant="secondaryAction">
              <FilePenLine className="size-4" />
              {t('researchGroup.detail.editGroup')}
            </Button>
          }
        />
      )}
    </div>
  );
}
