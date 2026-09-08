import { useTranslation } from 'react-i18next';
import { Button } from '@/components/ui/button';
import { Spinner } from '@/components/ui/spinner';

type ProjectSetupFooterActionsProps = Readonly<{
  canSaveSetup: boolean;
  isPreparingSetup: boolean;
  onBack: () => void;
  onSave: () => void;
}>;

export function ProjectSetupFooterActions({
  canSaveSetup,
  isPreparingSetup,
  onBack,
  onSave,
}: ProjectSetupFooterActionsProps) {
  const { t } = useTranslation();

  return (
    <div className="flex justify-end gap-3">
      <Button onClick={onBack} size="action" type="button" variant="secondaryAction">
        {t('project.create.previousStepSimple')}
      </Button>
      <Button
        disabled={!canSaveSetup}
        onClick={onSave}
        size="action-xl"
        type="button"
        variant="primaryAction"
      >
        {isPreparingSetup ? (
          <span className="inline-flex items-center gap-2">
            <Spinner aria-hidden className="size-4" />
            {t('project.create.savingSetup')}
          </span>
        ) : (
          t('project.create.nextStepSimple')
        )}
      </Button>
    </div>
  );
}
