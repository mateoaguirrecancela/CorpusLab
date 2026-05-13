import { useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { type FileRejection } from 'react-dropzone';
import { useNavigate, useSearchParams } from 'react-router';
import { toast } from 'sonner';
import {
  useAssignProjectParticipantsMutation,
  useConfigureProjectSetupMutation,
  useProjectCreateMutation,
  useUploadProjectDatasetMutation,
} from '@/modules/project/hooks/useProjectQueries';
import {
  cleanupIncompleteProject,
  getAssignParticipantsErrorMessage,
  getCreateProjectErrorMessage,
  getProjectSetupErrorMessage,
  getUploadDatasetErrorMessage,
} from '@/modules/project/services/projectService';
import {
  type ConfigureProjectSetupPayload,
  type ProjectParticipantAssignment,
} from '@/modules/project/types/project';
import { validateProjectFiles } from '@/modules/project/utils/projectCreateValidation';
import { useResearchGroupsQuery } from '@/modules/researchgroup/hooks/useResearchGroupQueries';
import { type ResearchGroupSummary } from '@/modules/researchgroup/types/researchGroup';

export type WizardStep = 1 | 2 | 3 | 4;

type ProjectCreatePageState = Readonly<{
  backFallbackPath: string;
  canCreateProject: boolean;
  canUploadDataset: boolean;
  currentStep: WizardStep;
  description: string;
  isFinalizingProject: boolean;
  isGroupLocked: boolean;
  isLoadingGroups: boolean;
  manageableGroups: ResearchGroupSummary[];
  name: string;
  numericGroupId: number;
  projectSetupPayload: ConfigureProjectSetupPayload | null;
  selectedFiles: File[];
  selectedGroupId: string;
  finalizeProject: (participantAssignments: ProjectParticipantAssignment[]) => Promise<void>;
  goToDatasetStep: () => void;
  goToInfoStep: () => void;
  goToSetupStep: () => void;
  rejectFiles: (fileRejections: FileRejection[]) => void;
  removeFile: (fileName: string, index: number) => void;
  selectFiles: (files: File[]) => void;
  setCurrentStep: (step: WizardStep) => void;
  setDescription: (description: string) => void;
  setName: (name: string) => void;
  setSelectedGroupId: (groupId: string) => void;
  setupCompleted: (payload: ConfigureProjectSetupPayload) => Promise<void>;
}>;

export function useProjectCreatePage(): ProjectCreatePageState {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const { data: groups = [], isLoading: isLoadingGroups } = useResearchGroupsQuery();
  const projectCreateMutation = useProjectCreateMutation();
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
    projectCreateMutation.isPending ||
    uploadDatasetMutation.isPending ||
    configureProjectSetupMutation.isPending ||
    assignProjectParticipantsMutation.isPending;

  const canCreateProject =
    Number.isFinite(numericGroupId) && numericGroupId > 0 && name.trim().length > 0;

  const canUploadDataset =
    Number.isFinite(numericGroupId) && numericGroupId > 0 && selectedFiles.length > 0;

  const selectFiles = (files: File[]) => {
    if (files.length === 0) {
      return;
    }

    const validatedFiles = validateProjectFiles({
      currentFiles: selectedFiles,
      incomingFiles: files,
      t,
    });

    if (validatedFiles) {
      setSelectedFiles(validatedFiles);
    }
  };

  const rejectFiles = (fileRejections: FileRejection[]) => {
    const firstRejectedFile = fileRejections[0]?.file;
    const extension = firstRejectedFile?.name.split('.').pop()?.toLowerCase();

    if (extension) {
      toast.error(t('project.create.forbiddenExtensionError', { extension }));
    }
  };

  const removeFile = (fileName: string, index: number) => {
    setSelectedFiles((prev) =>
      prev.filter((file, fileIndex) => !(file.name === fileName && fileIndex === index)),
    );
  };

  const goToDatasetStep = () => {
    if (!canCreateProject) {
      return;
    }

    setFinalizationProjectId(null);
    setCurrentStep(2);
  };

  const goToSetupStep = () => {
    if (!canUploadDataset) {
      return;
    }

    setFinalizationProjectId(null);
    setProjectSetupPayload(null);
    setCurrentStep(3);
  };

  const setupCompleted = async (payload: ConfigureProjectSetupPayload) => {
    setProjectSetupPayload(payload);
    setCurrentStep(4);
  };

  const ensureProjectCreatedForFinalization = async (): Promise<number | null> => {
    if (finalizationProjectId !== null) {
      return finalizationProjectId;
    }

    try {
      const createdProject = await projectCreateMutation.mutateAsync({
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
    participantAssignments: ProjectParticipantAssignment[],
  ): Promise<boolean> => {
    try {
      await assignProjectParticipantsMutation.mutateAsync({
        groupId: numericGroupId,
        projectId,
        payload: { participantAssignments },
      });
      return true;
    } catch (error) {
      toast.error(getAssignParticipantsErrorMessage(error));
      return false;
    }
  };

  const finalizeProject = async (participantAssignments: ProjectParticipantAssignment[]) => {
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

    try {
      const uploadedDataset = await uploadDatasetForProject(projectId);
      if (!uploadedDataset) {
        throw new Error('dataset_upload_failed');
      }

      const configuredSetup = await configureSetupForProject(projectId);
      if (!configuredSetup) {
        throw new Error('setup_config_failed');
      }

      const assignedParticipants = await assignParticipantsForProject(
        projectId,
        participantAssignments,
      );
      if (!assignedParticipants) {
        throw new Error('participant_assignment_failed');
      }

      if (isGroupLocked) {
        navigate(`/home/research-groups/${numericGroupId}`);
        return;
      }

      navigate(`/home/projects/${projectId}`);
    } catch (error) {
      const errorMessage = (error as Error).message;
      if (
        errorMessage === 'dataset_upload_failed' ||
        errorMessage === 'setup_config_failed' ||
        errorMessage === 'participant_assignment_failed'
      ) {
        try {
          setFinalizationProjectId(null);
          await cleanupIncompleteProject(numericGroupId, projectId);
        } catch (deleteError) {
          console.error('Failed to cleanup project after wizard error', deleteError);
        }
      }
    }
  };

  return {
    backFallbackPath,
    canCreateProject,
    canUploadDataset,
    currentStep,
    description,
    finalizeProject,
    goToDatasetStep,
    goToInfoStep: () => setCurrentStep(1),
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
    setSelectedGroupId: setCustomSelectedGroupId,
    setupCompleted,
  };
}
