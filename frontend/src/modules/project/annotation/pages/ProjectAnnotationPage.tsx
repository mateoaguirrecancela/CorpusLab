import { useTranslation } from 'react-i18next';
import { PageContainer } from '@/components/common/PageContainer';
import { Spinner } from '@/components/ui/spinner';
import { AnnotationCsvSourceContent } from '@/modules/project/annotation/components/AnnotationCsvSourceContent';
import { AnnotationEditorSidebar } from '@/modules/project/annotation/components/AnnotationEditorSidebar';
import { AnnotationFooterActions } from '@/modules/project/annotation/components/AnnotationFooterActions';
import { AnnotationGuidelineContent } from '@/modules/project/annotation/components/AnnotationGuidelineContent';
import { AnnotationGuidelinePanel } from '@/modules/project/annotation/components/AnnotationGuidelinePanel';
import { AnnotationSourcePanel } from '@/modules/project/annotation/components/AnnotationSourcePanel';
import { AnnotationWorkspaceHeader } from '@/modules/project/annotation/components/AnnotationWorkspaceHeader';
import { useProjectAnnotationPage } from '@/modules/project/annotation/hooks/useProjectAnnotationPage';

export default function ProjectAnnotationPage() {
  const { t } = useTranslation();
  const {
    activeNerLabel,
    annotationProjectType,
    annotationTargetColumn,
    annotationWorkspace,
    areStepActionsDisabled,
    canMoveNext,
    canMovePrevious,
    canRenderWorkspace,
    classificationHeading,
    completionColors,
    completionPercentage,
    csvLabelColumnValues,
    csvTargetColumnValue,
    currentDraft,
    currentGlobalStepIndex,
    currentStep,
    guidelinePdfError,
    guidelinePdfUrl,
    handleBackNavigation,
    handleFinishAction,
    handleNextAction,
    handlePreviousAction,
    handleToggleWarning,
    hasRenderableStep,
    isAnnotationWorkspaceLoading,
    isFirstStep,
    isGuidelineCollapsed,
    isInvalidProjectId,
    isLastStep,
    isProjectLoading,
    isReviewMode,
    isSavingCurrentStep,
    isSourceLoading,
    isWarningUpdating,
    labels,
    nerLabelColorMap,
    nerSourceSelectionRef,
    nerSourceText,
    nerTextSegments,
    numericProjectId,
    project,
    removeNerEntity,
    reviewedParticipantLabel,
    reviewedParticipantUserId,
    selectedLabels,
    setActiveNerLabel,
    setIsGuidelineCollapsed,
    shouldShowHeaderProgress,
    sourceFileName,
    sourceLoadError,
    sourceMimeType,
    sourceTextContent,
    sourceUrl,
    totalSteps,
    toggleLabel,
    updateDraft,
  } = useProjectAnnotationPage();

  return (
    <PageContainer className="mb-16">
      <AnnotationWorkspaceHeader
        annotationCompletedSteps={annotationWorkspace?.completedSteps ?? 0}
        annotationTotalSteps={annotationWorkspace?.totalSteps ?? 0}
        areStepActionsDisabled={areStepActionsDisabled}
        completionBarClassName={completionColors.bar}
        completionPercentage={completionPercentage}
        completionTextClassName={completionColors.text}
        fallbackTo={isInvalidProjectId ? '/home/projects' : `/home/projects/${numericProjectId}`}
        isReviewMode={isReviewMode}
        onBeforeNavigate={handleBackNavigation}
        reviewedParticipantLabel={reviewedParticipantLabel}
        reviewedParticipantUserId={reviewedParticipantUserId}
        shouldShowHeaderProgress={shouldShowHeaderProgress}
      />

      {(isProjectLoading || isAnnotationWorkspaceLoading) && (
        <div className="text-sm text-muted-foreground">
          <span className="inline-flex items-center gap-2">
            <Spinner aria-hidden className="size-4" />
            {t('project.annotationPage.loading')}
          </span>
        </div>
      )}

      {canRenderWorkspace && (
        <div className="space-y-4">
          {currentStep == null ? (
            <section className="rounded-xl border border-border bg-surface-base p-6">
              <p className="text-sm text-muted-foreground">
                {t('project.annotationPage.emptySteps')}
              </p>
            </section>
          ) : (
            <div className="grid gap-4 xl:grid-cols-[minmax(0,1fr)_360px]">
              <div className="min-w-0 space-y-4">
                <AnnotationGuidelinePanel
                  isCollapsed={isGuidelineCollapsed}
                  onToggleCollapsed={() => setIsGuidelineCollapsed((previous) => !previous)}
                >
                  <AnnotationGuidelineContent
                    guidelinePdfAvailable={project?.guidelinePdfAvailable}
                    guidelinePdfError={guidelinePdfError}
                    guidelinePdfUrl={guidelinePdfUrl}
                    guidelineText={project?.guidelineText}
                  />
                </AnnotationGuidelinePanel>

                <AnnotationSourcePanel
                  annotationProjectType={annotationProjectType}
                  csvSourceContent={
                    <AnnotationCsvSourceContent
                      annotationProjectType={annotationProjectType}
                      annotationTargetColumn={annotationTargetColumn}
                      csvLabelColumnValues={csvLabelColumnValues}
                      csvTargetColumnValue={csvTargetColumnValue}
                      currentStep={currentStep}
                      isReviewMode={isReviewMode}
                      nerLabelColorMap={nerLabelColorMap}
                      nerSourceSelectionRef={nerSourceSelectionRef}
                      nerTextSegments={nerTextSegments}
                      onRemoveNerEntity={removeNerEntity}
                    />
                  }
                  currentGlobalStepIndex={currentGlobalStepIndex}
                  currentStep={currentStep}
                  isReviewMode={isReviewMode}
                  isSourceLoading={isSourceLoading}
                  isWarningUpdating={isWarningUpdating}
                  nerLabelColorMap={nerLabelColorMap}
                  nerSourceSelectionRef={nerSourceSelectionRef}
                  nerSourceText={nerSourceText}
                  nerTextSegments={nerTextSegments}
                  onRemoveNerEntity={removeNerEntity}
                  onToggleWarning={handleToggleWarning}
                  sourceFileName={sourceFileName}
                  sourceLoadError={sourceLoadError}
                  sourceMimeType={sourceMimeType}
                  sourceTextContent={sourceTextContent}
                  sourceUrl={sourceUrl}
                  totalSteps={totalSteps}
                />
              </div>

              <AnnotationEditorSidebar
                activeNerLabel={activeNerLabel}
                annotationProjectType={annotationProjectType}
                classificationHeading={classificationHeading}
                currentDraft={currentDraft}
                currentStep={currentStep}
                isReviewMode={isReviewMode}
                labels={labels}
                onActiveNerLabelChange={setActiveNerLabel}
                onToggleLabel={toggleLabel}
                onUpdateDraft={updateDraft}
                project={project}
                selectedLabels={selectedLabels}
              />
            </div>
          )}
        </div>
      )}

      {hasRenderableStep && canRenderWorkspace && (
        <AnnotationFooterActions
          areStepActionsDisabled={areStepActionsDisabled}
          canMoveNext={canMoveNext}
          canMovePrevious={canMovePrevious}
          isFirstStep={isFirstStep}
          isLastStep={isLastStep}
          isSavingCurrentStep={isSavingCurrentStep}
          onFinish={() => void handleFinishAction()}
          onNext={() => void handleNextAction()}
          onPrevious={() => void handlePreviousAction()}
        />
      )}
    </PageContainer>
  );
}
