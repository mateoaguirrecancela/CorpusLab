import { useTranslation } from 'react-i18next';
import { BackButton } from '@/components/common/BackButton';
import { PageContainer } from '@/components/common/PageContainer';
import { Spinner } from '@/components/ui/spinner';
import { ProjectCreateDatasetStep } from '@/modules/project/setup/components/ProjectCreateDatasetStep';
import { ProjectCreateInfoStep } from '@/modules/project/setup/components/ProjectCreateInfoStep';
import { ProjectCreateProgress } from '@/modules/project/setup/components/ProjectCreateProgress';
import { ProjectAssignmentStep } from '@/modules/project/participants/components/ProjectAssignmentStep';
import { ProjectSetupStep } from '@/modules/project/setup/components/ProjectSetupStep';
import { useProjectCreatePage } from '@/modules/project/setup/hooks/useProjectCreatePage';
import { isPositiveId } from '@/modules/project/shared/utils/projectFormUtils';

export default function ProjectCreatePage() {
  const { t } = useTranslation();
  const {
    backFallbackPath,
    canCreateProject,
    canUploadDataset,
    currentStep,
    description,
    descriptionErrorMessage,
    finalizeProject,
    goToDatasetStep,
    goToInfoStep,
    goToSetupStep,
    isFinalizingProject,
    isGroupLocked,
    isLoadingGroups,
    manageableGroups,
    name,
    nameErrorMessage,
    numericGroupId,
    projectSetupPayload,
    rejectFiles,
    removeFile,
    selectFiles,
    selectedFiles,
    selectedGroupId,
    selectedGroupErrorMessage,
    setCurrentStep,
    setDescription,
    setName,
    setSelectedGroupId,
    setupCompleted,
  } = useProjectCreatePage();

  const mainStepContent = (() => {
    if (isLoadingGroups) {
      return (
        <div className="mt-8 rounded-md border border-border bg-background px-4 py-6 text-sm text-muted-foreground">
          <span className="inline-flex items-center gap-2">
            <Spinner aria-hidden className="size-4" />
            {t('project.create.loadingGroups')}
          </span>
        </div>
      );
    }

    if (manageableGroups.length === 0) {
      return (
        <p className="mt-8 rounded-md border border-dashed border-border bg-background px-4 py-5 text-sm text-muted-foreground">
          {t('project.create.noGroups')}
        </p>
      );
    }

    if (currentStep === 1) {
      return (
        <ProjectCreateInfoStep
          canContinue={canCreateProject}
          description={description}
          descriptionErrorMessage={descriptionErrorMessage}
          groups={manageableGroups}
          isGroupLocked={isGroupLocked}
          name={name}
          nameErrorMessage={nameErrorMessage}
          onContinue={goToDatasetStep}
          onDescriptionChange={setDescription}
          onGroupChange={setSelectedGroupId}
          onNameChange={setName}
          selectedGroupErrorMessage={selectedGroupErrorMessage}
          selectedGroupId={selectedGroupId}
        />
      );
    }

    if (currentStep === 2) {
      return (
        <ProjectCreateDatasetStep
          canContinue={canUploadDataset}
          onBack={goToInfoStep}
          onContinue={goToSetupStep}
          onFilesRejected={rejectFiles}
          onFilesSelected={selectFiles}
          onRemoveFile={removeFile}
          selectedFiles={selectedFiles}
        />
      );
    }

    if (currentStep === 3 && isPositiveId(numericGroupId)) {
      return (
        <ProjectSetupStep
          datasetFiles={selectedFiles}
          onBack={() => setCurrentStep(2)}
          onCompleted={(payload) => void setupCompleted(payload)}
        />
      );
    }

    if (currentStep === 4 && isPositiveId(numericGroupId) && projectSetupPayload !== null) {
      return (
        <ProjectAssignmentStep
          groupId={numericGroupId}
          isSubmitting={isFinalizingProject}
          onBack={() => setCurrentStep(3)}
          onCompleted={(participantAssignments) => void finalizeProject(participantAssignments)}
        />
      );
    }

    return null;
  })();

  return (
    <PageContainer>
      <div className="space-y-4">
        <div className="flex items-center justify-between gap-3">
          <BackButton fallbackTo={backFallbackPath} />
        </div>

        <section className="rounded-md border border-border bg-surface-base p-6 sm:p-7">
          <h1 className="text-3xl font-black tracking-tight text-primary sm:text-4xl">
            {t('project.create.pageTitle')}
          </h1>

          <div className="mt-8">
            <ProjectCreateProgress currentStep={currentStep} />

            {mainStepContent}
          </div>
        </section>
      </div>
    </PageContainer>
  );
}
