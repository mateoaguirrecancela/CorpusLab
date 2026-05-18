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
      <Button
        className="h-10 rounded-md border border-border bg-surface-base px-6 text-sm font-semibold text-primary hover:bg-accent cursor-pointer"
        onClick={onBack}
        type="button"
        variant="outline"
      >
        {t('project.create.previousStepSimple')}
      </Button>
      <Button
        className="h-10 min-w-44 rounded-md bg-primary text-sm font-semibold text-white transition-colors hover:bg-primary-strong disabled:bg-secondary cursor-pointer"
        disabled={!canSaveSetup}
        onClick={onSave}
        type="button"
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
