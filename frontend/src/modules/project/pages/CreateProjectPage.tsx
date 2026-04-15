import { type ReactNode, useMemo, useState } from 'react';
import { X } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { useNavigate, useSearchParams } from 'react-router';
import { toast } from 'sonner';
import { BackButton } from '@/components/common/BackButton';
import { FormFieldControl } from '@/components/common/FormFieldControl';
import { PageContainer } from '@/components/common/PageContainer';
import { Button } from '@/components/ui/button';
import { Spinner } from '@/components/ui/spinner';
import { ProjectAssignmentStep } from '@/modules/project/components/ProjectAssignmentStep';
import { ProjectSetupStep } from '@/modules/project/components/ProjectSetupStep';
import { UploadDropzone } from '@/modules/project/components/UploadDropzone';
import {
  useCreateProjectMutation,
  useUploadProjectDatasetMutation,
} from '@/modules/project/hooks/useProjectQueries';
import {
  getCreateProjectErrorMessage,
  getUploadDatasetErrorMessage,
} from '@/modules/project/services/projectService';
import { useResearchGroupsQuery } from '@/modules/researchgroup/hooks/useResearchGroupQueries';

function formatFileSize(bytes: number): string {
  if (bytes < 1024) {
    return `${bytes} B`;
  }
  if (bytes < 1024 * 1024) {
    return `${(bytes / 1024).toFixed(1)} KB`;
  }
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

// eslint-disable-next-line sonarjs/cognitive-complexity
export default function CreateProjectPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const { data: groups = [], isLoading: isLoadingGroups } = useResearchGroupsQuery();
  const createProjectMutation = useCreateProjectMutation();
  const uploadDatasetMutation = useUploadProjectDatasetMutation();

  const manageableGroups = useMemo(
    () => groups.filter((group) => group.role === 'OWNER' || group.role === 'ADMIN'),
    [groups],
  );

  const initialGroupId = Number(searchParams.get('groupId'));
  const hasInitialGroupId = Number.isFinite(initialGroupId) && initialGroupId > 0;

  const lockedGroup = useMemo(
    () => manageableGroups.find((group) => group.id === initialGroupId),
    [manageableGroups, initialGroupId],
  );

  const isGroupLocked = hasInitialGroupId && Boolean(lockedGroup);
  const backFallbackPath =
    isGroupLocked && lockedGroup ? `/home/research-groups/${lockedGroup.id}` : '/home/projects';

  const [customSelectedGroupId, setCustomSelectedGroupId] = useState('');
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [currentStep, setCurrentStep] = useState<1 | 2 | 3 | 4>(1);
  const [createdProjectId, setCreatedProjectId] = useState<number | null>(null);
  const [selectedFiles, setSelectedFiles] = useState<File[]>([]);

  const selectedGroupId = useMemo(() => {
    if (isGroupLocked && lockedGroup) {
      return String(lockedGroup.id);
    }

    if (customSelectedGroupId.length > 0) {
      return customSelectedGroupId;
    }

    if (manageableGroups.length > 0) {
      return String(manageableGroups[0].id);
    }

    return '';
  }, [customSelectedGroupId, isGroupLocked, lockedGroup, manageableGroups]);

  const numericGroupId = Number(selectedGroupId);
  const isSavingProject = createProjectMutation.isPending;
  const isUploadingDataset = uploadDatasetMutation.isPending;

  const canCreateProject =
    Number.isFinite(numericGroupId) &&
    numericGroupId > 0 &&
    name.trim().length > 0 &&
    !isSavingProject;

  const canUploadDataset =
    Number.isFinite(numericGroupId) &&
    numericGroupId > 0 &&
    createdProjectId !== null &&
    selectedFiles.length > 0 &&
    !isUploadingDataset;

  const totalSteps = 4;
  const progressPercentage = ((currentStep - 1) / (totalSteps - 1)) * 100;

  const handleFilesSelected = (files: FileList | null) => {
    if (!files) {
      return;
    }

    const incomingFiles = Array.from(files);
    setSelectedFiles((prev) => [...prev, ...incomingFiles]);
  };

  const handleRemoveFile = (fileName: string, index: number) => {
    setSelectedFiles((prev) =>
      prev.filter((file, fileIndex) => !(file.name === fileName && fileIndex === index)),
    );
  };

  const handleCreateProject = async () => {
    if (!canCreateProject) {
      return;
    }

    try {
      const createdProject = await createProjectMutation.mutateAsync({
        groupId: numericGroupId,
        payload: {
          name: name.trim(),
          description: description.trim() || undefined,
        },
      });

      setCreatedProjectId(createdProject.id);
      setCurrentStep(2);
    } catch (error) {
      toast.error(getCreateProjectErrorMessage(error));
    }
  };

  const handleUploadDataset = async () => {
    if (!canUploadDataset || createdProjectId === null) {
      return;
    }

    try {
      await uploadDatasetMutation.mutateAsync({
        groupId: numericGroupId,
        projectId: createdProjectId,
        files: selectedFiles,
      });

      setCurrentStep(3);
    } catch (error) {
      toast.error(getUploadDatasetErrorMessage(error));
    }
  };

  let mainStepContent: ReactNode;
  if (isLoadingGroups) {
    mainStepContent = (
      <div className="mt-8 rounded-md border border-border bg-background px-4 py-6 text-sm text-muted-foreground">
        <span className="inline-flex items-center gap-2">
          <Spinner aria-hidden className="size-4" />
          {t('project.create.loadingGroups')}
        </span>
      </div>
    );
  } else if (manageableGroups.length === 0) {
    mainStepContent = (
      <p className="mt-8 rounded-md border border-dashed border-border bg-background px-4 py-5 text-sm text-muted-foreground">
        {t('project.create.noGroups')}
      </p>
    );
  } else if (currentStep === 1) {
    mainStepContent = (
      <div className="mt-8 space-y-4">
        <FormFieldControl
          controlType="select"
          id="create-project-group"
          label={t('project.create.groupLabel')}
          onValueChange={setCustomSelectedGroupId}
          options={manageableGroups.map((group) => ({
            label: group.name,
            value: String(group.id),
          }))}
          required
          selectProps={{ disabled: isGroupLocked, required: true }}
          value={selectedGroupId}
        />

        <FormFieldControl
          id="create-project-name-page"
          inputProps={{
            maxLength: 256,
            placeholder: t('project.create.namePlaceholder'),
            required: true,
          }}
          label={t('project.create.nameLabel')}
          onValueChange={setName}
          required
          value={name}
        />

        <FormFieldControl
          controlType="textarea"
          id="create-project-description-page"
          label={t('project.create.descriptionLabel')}
          onValueChange={setDescription}
          textareaProps={{
            maxLength: 2048,
            placeholder: t('project.create.descriptionPlaceholder'),
          }}
          value={description}
        />

        <div className="flex justify-end gap-3">
          <Button
            className="h-10 min-w-36 rounded-md bg-primary text-sm font-semibold text-white transition-colors hover:bg-primary-strong disabled:bg-secondary cursor-pointer"
            disabled={!canCreateProject}
            onClick={() => void handleCreateProject()}
            type="button"
          >
            {isSavingProject ? (
              <span className="inline-flex items-center gap-2">
                <Spinner aria-hidden className="size-4" />
                {t('common.actions.saving')}
              </span>
            ) : (
              t('project.create.nextStepSimple')
            )}
          </Button>
        </div>
      </div>
    );
  } else if (currentStep === 2) {
    mainStepContent = (
      <div className="mt-8 space-y-3">
        <p className="text-xs font-bold uppercase tracking-wider text-muted-foreground">
          {t('project.create.steps.dataset')} *
        </p>

        <UploadDropzone
          accept=".pdf,.txt,.json,.csv,image/*"
          description={t('project.create.selectFilesHint')}
          multiple
          onFilesChange={handleFilesSelected}
          title={t('project.create.selectFiles')}
        />

        {selectedFiles.length > 0 && (
          <div className="space-y-2 rounded-md border border-border bg-background p-3">
            {selectedFiles.map((file, index) => (
              <div
                className="flex items-center justify-between gap-3"
                key={`${file.name}-${index}`}
              >
                <div className="min-w-0">
                  <p className="truncate text-sm font-semibold text-primary">{file.name}</p>
                  <p className="text-xs text-muted-foreground">{formatFileSize(file.size)}</p>
                </div>
                <button
                  aria-label={t('project.create.removeFile')}
                  className="inline-flex size-8 items-center justify-center rounded-md text-muted-foreground transition-colors hover:bg-accent hover:text-primary"
                  onClick={() => handleRemoveFile(file.name, index)}
                  type="button"
                >
                  <X className="size-4" />
                </button>
              </div>
            ))}
          </div>
        )}

        <div className="flex justify-end gap-3">
          <Button
            className="h-10 rounded-md border border-border bg-surface-base px-6 text-sm font-semibold text-primary hover:bg-accent cursor-pointer"
            onClick={() => setCurrentStep(1)}
            type="button"
            variant="outline"
          >
            {t('project.create.previousStepSimple')}
          </Button>
          <Button
            className="h-10 min-w-36 rounded-md bg-primary text-sm font-semibold text-white transition-colors hover:bg-primary-strong disabled:bg-secondary cursor-pointer"
            disabled={!canUploadDataset}
            onClick={() => void handleUploadDataset()}
            type="button"
          >
            {isUploadingDataset ? (
              <span className="inline-flex items-center gap-2">
                <Spinner aria-hidden className="size-4" />
                {t('project.create.uploadingDataset')}
              </span>
            ) : (
              t('project.create.nextStepSimple')
            )}
          </Button>
        </div>
      </div>
    );
  } else if (
    currentStep === 3 &&
    createdProjectId !== null &&
    Number.isFinite(numericGroupId) &&
    numericGroupId > 0
  ) {
    mainStepContent = (
      <ProjectSetupStep
        groupId={numericGroupId}
        onBack={() => setCurrentStep(2)}
        onCompleted={() => setCurrentStep(4)}
        projectId={createdProjectId}
      />
    );
  } else if (
    currentStep === 4 &&
    createdProjectId !== null &&
    Number.isFinite(numericGroupId) &&
    numericGroupId > 0
  ) {
    mainStepContent = (
      <ProjectAssignmentStep
        groupId={numericGroupId}
        onBack={() => setCurrentStep(3)}
        onCompleted={() => {
          if (isGroupLocked) {
            navigate(`/home/research-groups/${numericGroupId}`);
            return;
          }

          navigate(`/home/projects/${createdProjectId}`);
        }}
        projectId={createdProjectId}
      />
    );
  } else {
    mainStepContent = null;
  }

  return (
    <PageContainer className="py-4 sm:py-6">
      <div className="space-y-4">
        <div className="flex items-center justify-between gap-3">
          <BackButton fallbackTo={backFallbackPath} />
        </div>

        <section className="rounded-md border border-border bg-surface-base p-6 sm:p-7">
          <h1 className="text-3xl font-black tracking-tight text-primary sm:text-4xl">
            {t('project.create.pageTitle')}
          </h1>

          <div className="mt-8">
            <div className="relative mb-6">
              <div className="absolute top-1/2 left-0 right-0 h-px -translate-y-1/2 bg-border" />
              <div
                className="absolute top-1/2 left-0 h-px -translate-y-1/2 bg-primary transition-all duration-500"
                style={{ width: `${progressPercentage}%` }}
              />

              <div className="relative flex items-center justify-between">
                {[1, 2, 3, 4].map((step) => {
                  const isCompleted = step <= currentStep;

                  return (
                    <div
                      className={[
                        'size-4 rounded-full border-2 transition-colors duration-500 z-10',
                        isCompleted ? 'border-primary bg-primary' : 'border-border bg-surface-base',
                      ].join(' ')}
                      key={step}
                    />
                  );
                })}
              </div>
            </div>

            {mainStepContent}
          </div>
        </section>
      </div>
    </PageContainer>
  );
}
