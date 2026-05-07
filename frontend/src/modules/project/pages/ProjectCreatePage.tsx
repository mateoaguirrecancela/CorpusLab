import { useTranslation } from 'react-i18next';
import { BackButton } from '@/components/common/BackButton';
import { PageContainer } from '@/components/common/PageContainer';
import { Spinner } from '@/components/ui/spinner';
import { ProjectCreateDatasetStep } from '@/modules/project/components/ProjectCreateDatasetStep';
import { ProjectCreateInfoStep } from '@/modules/project/components/ProjectCreateInfoStep';
import { ProjectCreateProgress } from '@/modules/project/components/ProjectCreateProgress';
import { ProjectAssignmentStep } from '@/modules/project/components/ProjectAssignmentStep';
import { ProjectSetupStep } from '@/modules/project/components/ProjectSetupStep';
import { useProjectCreatePage } from '@/modules/project/hooks/useProjectCreatePage';

export default function ProjectCreatePage() {
  const { t } = useTranslation();
  const {
    backFallbackPath,
    canCreateProject,
    canUploadDataset,
    currentStep,
    description,
    finalizeProject,
    goToDatasetStep,
    goToInfoStep,
    goToSetupStep,
    isFinalizingProject,
    isGroupLocked,
    isLoadingGroups,
    manageableGroups,
    name,
    numericGroupId,
    projectSetupPayload,
    rejectFiles,
    removeFile,
    selectFiles,
    selectedFiles,
    selectedGroupId,
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
          groups={manageableGroups}
          isGroupLocked={isGroupLocked}
          name={name}
          onContinue={goToDatasetStep}
          onDescriptionChange={setDescription}
          onGroupChange={setSelectedGroupId}
          onNameChange={setName}
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

    if (currentStep === 3 && Number.isFinite(numericGroupId) && numericGroupId > 0) {
      return (
        <ProjectSetupStep
          datasetFiles={selectedFiles}
          onBack={() => setCurrentStep(2)}
          onCompleted={(payload) => void setupCompleted(payload)}
        />
      );
    }

    if (
      currentStep === 4 &&
      Number.isFinite(numericGroupId) &&
      numericGroupId > 0 &&
      projectSetupPayload !== null
    ) {
      return (
        <ProjectAssignmentStep
          groupId={numericGroupId}
          isSubmitting={isFinalizingProject}
          onBack={() => setCurrentStep(3)}
          onCompleted={(participantUserIds) => void finalizeProject(participantUserIds)}
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
