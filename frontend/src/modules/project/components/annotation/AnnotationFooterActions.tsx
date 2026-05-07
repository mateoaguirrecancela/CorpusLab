import { ArrowLeft, ArrowRight } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Button } from '@/components/ui/button';
import { Spinner } from '@/components/ui/spinner';

type AnnotationFooterActionsProps = Readonly<{
  areStepActionsDisabled: boolean;
  canMoveNext: boolean;
  canMovePrevious: boolean;
  isFirstStep: boolean;
  isLastStep: boolean;
  isSavingCurrentStep: boolean;
  onFinish: () => void;
  onNext: () => void;
  onPrevious: () => void;
}>;

export function AnnotationFooterActions({
  areStepActionsDisabled,
  canMoveNext,
  canMovePrevious,
  isFirstStep,
  isLastStep,
  isSavingCurrentStep,
  onFinish,
  onNext,
  onPrevious,
}: AnnotationFooterActionsProps) {
  const { t } = useTranslation();

  return (
    <div className="fixed inset-x-0 bottom-0 z-20 border-t border-border bg-surface-base/95 backdrop-blur-sm">
      <div className="mx-auto flex w-full max-w-7xl items-center justify-end gap-2 px-4 py-3 sm:px-6">
        {!isFirstStep && (
          <Button
            disabled={!canMovePrevious || areStepActionsDisabled}
            onClick={onPrevious}
            type="button"
            className="h-10 rounded-md border bg-surface-base px-6 text-sm font-semibold text-primary hover:bg-accent cursor-pointer"
            variant="outline"
          >
            <ArrowLeft className="size-4" />
            {t('project.annotationPage.previous')}
          </Button>
        )}

        {isLastStep ? (
          <Button
            disabled={areStepActionsDisabled}
            onClick={onFinish}
            type="button"
            className="h-10 rounded-md bg-primary px-6 text-sm font-semibold text-white transition-colors hover:bg-primary-strong disabled:bg-secondary cursor-pointer"
          >
            {isSavingCurrentStep && <Spinner aria-hidden className="size-4" />}
            {t('project.annotationPage.finish')}
          </Button>
        ) : (
          <Button
            disabled={!canMoveNext || areStepActionsDisabled}
            onClick={onNext}
            type="button"
            className="h-10 rounded-md bg-primary px-6 text-sm font-semibold text-white transition-colors hover:bg-primary-strong disabled:bg-secondary cursor-pointer"
          >
            {isSavingCurrentStep && <Spinner aria-hidden className="size-4" />}
            {t('project.annotationPage.next')}
            <ArrowRight className="size-4" />
          </Button>
        )}
      </div>
    </div>
  );
}
