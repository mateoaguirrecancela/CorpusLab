import { type ReactNode, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate, useSearchParams } from 'react-router';
import { toast } from 'sonner';
import { BackButton } from '@/components/common/BackButton';
import { PageContainer } from '@/components/common/PageContainer';
import { Spinner } from '@/components/ui/spinner';
import { CreateProjectDatasetStep } from '@/modules/project/components/CreateProjectDatasetStep';
import { CreateProjectInfoStep } from '@/modules/project/components/CreateProjectInfoStep';
import { CreateProjectProgress } from '@/modules/project/components/CreateProjectProgress';
import { ProjectAssignmentStep } from '@/modules/project/components/ProjectAssignmentStep';
import { ProjectSetupStep } from '@/modules/project/components/ProjectSetupStep';
import {
  useAssignProjectParticipantsMutation,
  useConfigureProjectSetupMutation,
  useCreateProjectMutation,
  useUploadProjectDatasetMutation,
} from '@/modules/project/hooks/useProjectQueries';
import {
  getAssignParticipantsErrorMessage,
  getCreateProjectErrorMessage,
  getProjectSetupErrorMessage,
  getUploadDatasetErrorMessage,
} from '@/modules/project/services/projectService';
import { type ConfigureProjectSetupPayload } from '@/modules/project/types/project';
import { useResearchGroupsQuery } from '@/modules/researchgroup/hooks/useResearchGroupQueries';

type WizardStep = 1 | 2 | 3 | 4;

// eslint-disable-next-line sonarjs/cognitive-complexity
export default function CreateProjectPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const { data: groups = [], isLoading: isLoadingGroups } = useResearchGroupsQuery();
  const createProjectMutation = useCreateProjectMutation();
  const uploadDatasetMutation = useUploadProjectDatasetMutation();
  const configureProjectSetupMutation = useConfigureProjectSetupMutation();
  const assignProjectParticipantsMutation = useAssignProjectParticipantsMutation();

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
  const [currentStep, setCurrentStep] = useState<WizardStep>(1);
  const [finalizationProjectId, setFinalizationProjectId] = useState<number | null>(null);
  const [selectedFiles, setSelectedFiles] = useState<File[]>([]);
  const [projectSetupPayload, setProjectSetupPayload] =
    useState<ConfigureProjectSetupPayload | null>(null);

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
  const isFinalizingProject =
    createProjectMutation.isPending ||
    uploadDatasetMutation.isPending ||
    configureProjectSetupMutation.isPending ||
    assignProjectParticipantsMutation.isPending;

  const canCreateProject =
    Number.isFinite(numericGroupId) && numericGroupId > 0 && name.trim().length > 0;

  const canUploadDataset =
    Number.isFinite(numericGroupId) && numericGroupId > 0 && selectedFiles.length > 0;

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

  const setupDatasetItems = useMemo(
    () =>
      selectedFiles.map((file) => ({
        fileName: file.name,
        mimeType: file.type || 'application/octet-stream',
      })),
    [selectedFiles],
  );

  const handleCreateProject = () => {
    if (!canCreateProject) {
      return;
    }

    setFinalizationProjectId(null);
    setCurrentStep(2);
  };

  const handleUploadDataset = () => {
    if (!canUploadDataset) {
      return;
    }

    setFinalizationProjectId(null);
    setProjectSetupPayload(null);
    setCurrentStep(3);
  };

  const handleSetupCompleted = async (payload: ConfigureProjectSetupPayload) => {
    setProjectSetupPayload(payload);
    setCurrentStep(4);
  };

  const ensureProjectCreatedForFinalization = async (): Promise<number | null> => {
    if (finalizationProjectId !== null) {
      return finalizationProjectId;
    }

    try {
      const createdProject = await createProjectMutation.mutateAsync({
        groupId: numericGroupId,
        payload: {
          name: name.trim(),
          description: description.trim() || undefined,
        },
      });

      setFinalizationProjectId(createdProject.id);
      return createdProject.id;
    } catch (error) {
      toast.error(getCreateProjectErrorMessage(error));
      return null;
    }
  };

  const uploadDatasetForProject = async (projectId: number): Promise<boolean> => {
    try {
      await uploadDatasetMutation.mutateAsync({
        groupId: numericGroupId,
        projectId,
        files: selectedFiles,
      });
      return true;
    } catch (error) {
      toast.error(getUploadDatasetErrorMessage(error));
      return false;
    }
  };

  const configureSetupForProject = async (projectId: number): Promise<boolean> => {
    if (!projectSetupPayload) {
      return false;
    }

    try {
      await configureProjectSetupMutation.mutateAsync({
        groupId: numericGroupId,
        projectId,
        payload: projectSetupPayload,
      });
      return true;
    } catch (error) {
      toast.error(getProjectSetupErrorMessage(error));
      return false;
    }
  };

  const assignParticipantsForProject = async (
    projectId: number,
    participantUserIds: number[],
  ): Promise<boolean> => {
    try {
      await assignProjectParticipantsMutation.mutateAsync({
        groupId: numericGroupId,
        projectId,
        participantUserIds,
      });
      return true;
    } catch (error) {
      toast.error(getAssignParticipantsErrorMessage(error));
      return false;
    }
  };

  const handleFinalizeProject = async (participantUserIds: number[]) => {
    if (
      !canCreateProject ||
      !canUploadDataset ||
      !projectSetupPayload ||
      Number.isFinite(numericGroupId) === false ||
      numericGroupId <= 0
    ) {
      return;
    }

    const projectId = await ensureProjectCreatedForFinalization();
    if (projectId === null) {
      return;
    }

    const uploadedDataset = await uploadDatasetForProject(projectId);
    if (!uploadedDataset) {
      return;
    }

    const configuredSetup = await configureSetupForProject(projectId);
    if (!configuredSetup) {
      return;
    }

    const assignedParticipants = await assignParticipantsForProject(projectId, participantUserIds);
    if (!assignedParticipants) {
      return;
    }

    if (isGroupLocked) {
      navigate(`/home/research-groups/${numericGroupId}`);
      return;
    }

    navigate(`/home/projects/${projectId}`);
  };

  const renderMainStepContent = (): ReactNode => {
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
        <CreateProjectInfoStep
          canContinue={canCreateProject}
          description={description}
          groups={manageableGroups}
          isGroupLocked={isGroupLocked}
          name={name}
          onContinue={handleCreateProject}
          onDescriptionChange={setDescription}
          onGroupChange={setCustomSelectedGroupId}
          onNameChange={setName}
          selectedGroupId={selectedGroupId}
        />
      );
    }

    if (currentStep === 2) {
      return (
        <CreateProjectDatasetStep
          canContinue={canUploadDataset}
          onBack={() => setCurrentStep(1)}
          onContinue={handleUploadDataset}
          onFilesSelected={handleFilesSelected}
          onRemoveFile={handleRemoveFile}
          selectedFiles={selectedFiles}
        />
      );
    }

    if (currentStep === 3 && Number.isFinite(numericGroupId) && numericGroupId > 0) {
      return (
        <ProjectSetupStep
          datasetItems={setupDatasetItems}
          onBack={() => setCurrentStep(2)}
          onCompleted={handleSetupCompleted}
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
          onCompleted={handleFinalizeProject}
        />
      );
    }

    return null;
  };

  const mainStepContent = renderMainStepContent();

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
            <CreateProjectProgress currentStep={currentStep} />

            {mainStepContent}
          </div>
        </section>
      </div>
    </PageContainer>
  );
}
