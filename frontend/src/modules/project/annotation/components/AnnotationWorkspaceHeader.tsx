import { BackButton } from '@/components/common/BackButton';

type AnnotationWorkspaceHeaderProps = Readonly<{
  annotationCompletedSteps: number;
  annotationTotalSteps: number;
  areStepActionsDisabled: boolean;
  completionBarClassName: string;
  completionPercentage: number;
  completionTextClassName: string;
  fallbackTo: string;
  isReviewMode: boolean;
  reviewedParticipantLabel: string | null;
  reviewedParticipantUserId: number | null;
  shouldShowHeaderProgress: boolean;
  onBeforeNavigate: () => Promise<boolean>;
}>;

export function AnnotationWorkspaceHeader({
  annotationCompletedSteps,
  annotationTotalSteps,
  areStepActionsDisabled,
  completionBarClassName,
  completionPercentage,
  completionTextClassName,
  fallbackTo,
  isReviewMode,
  reviewedParticipantLabel,
  reviewedParticipantUserId,
  shouldShowHeaderProgress,
  onBeforeNavigate,
}: AnnotationWorkspaceHeaderProps) {
  return (
    <>
      {isReviewMode && (
        <h1 className="text-3xl font-black tracking-tight text-primary sm:text-4xl">
          {reviewedParticipantLabel ?? String(reviewedParticipantUserId)}
        </h1>
      )}

      <div className="flex items-center justify-between gap-3">
        <BackButton
          disabled={areStepActionsDisabled}
          fallbackTo={fallbackTo}
          onBeforeNavigate={onBeforeNavigate}
        />
        {shouldShowHeaderProgress && (
          <div className="w-full max-w-64 space-y-2">
            <div className="flex items-center justify-between text-xs font-semibold uppercase tracking-wider text-muted-foreground">
              <span className="text-xs text-muted-foreground">
                {annotationCompletedSteps}/{annotationTotalSteps}
              </span>
              <span className={completionTextClassName}>{completionPercentage}%</span>
            </div>
            <div className="h-2.5 w-full overflow-hidden rounded-full bg-muted/60">
              <div
                className={`h-full rounded-full transition-all duration-500 ${completionBarClassName}`}
                style={{ width: `${completionPercentage}%` }}
              />
            </div>
          </div>
        )}
      </div>
    </>
  );
}
