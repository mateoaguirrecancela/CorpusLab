import { useEffect, useMemo, useState } from 'react';
import { zodResolver } from '@hookform/resolvers/zod';
import { useTranslation } from 'react-i18next';
import { type FileRejection } from 'react-dropzone';
import { useForm, useWatch } from 'react-hook-form';
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
import {
  createProjectInfoSchema,
  type ProjectCreateInfoFormValues,
} from '@/modules/project/schemas/projectFormSchemas';
import { validateProjectFiles } from '@/modules/project/utils/projectCreateValidation';
import {
  DIRTY_VALIDATED_FIELD_OPTIONS,
  hasText,
  isPositiveId,
  toProjectPayload,
} from '@/modules/project/utils/projectFormUtils';
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
  descriptionErrorMessage?: string;
  finalizeProject: (participantAssignments: ProjectParticipantAssignment[]) => Promise<void>;
  goToDatasetStep: () => void;
  goToInfoStep: () => void;
  goToSetupStep: () => void;
  nameErrorMessage?: string;
  rejectFiles: (fileRejections: FileRejection[]) => void;
  removeFile: (fileName: string, index: number) => void;
  selectFiles: (files: File[]) => void;
  selectedGroupErrorMessage?: string;
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
  const projectInfoSchema = useMemo(() => createProjectInfoSchema(t), [t]);
  const projectInfoForm = useForm<ProjectCreateInfoFormValues>({
    defaultValues: {
      description: '',
      name: '',
      selectedGroupId: '',
    },
    mode: 'onChange',
    resolver: zodResolver(projectInfoSchema),
  });
  const {
    formState: { errors: projectInfoErrors, isValid: isProjectInfoValid },
    control,
    getValues: getProjectInfoValues,
    setValue: setProjectInfoValue,
    trigger: triggerProjectInfo,
  } = projectInfoForm;

  const manageableGroups = useMemo(
    () => groups.filter((group) => group.role === 'OWNER' || group.role === 'ADMIN'),
    [groups],
  );

  const initialGroupId = Number(searchParams.get('groupId'));
  const hasInitialGroupId = isPositiveId(initialGroupId);

  const lockedGroup = useMemo(
    () => manageableGroups.find((group) => group.id === initialGroupId),
    [manageableGroups, initialGroupId],
  );

  const isGroupLocked = hasInitialGroupId && Boolean(lockedGroup);
  const backFallbackPath =
    isGroupLocked && lockedGroup ? `/home/research-groups/${lockedGroup.id}` : '/home/projects';

  const [currentStep, setCurrentStep] = useState<WizardStep>(1);
  const [finalizationProjectId, setFinalizationProjectId] = useState<number | null>(null);
  const [selectedFiles, setSelectedFiles] = useState<File[]>([]);
  const [projectSetupPayload, setProjectSetupPayload] =
    useState<ConfigureProjectSetupPayload | null>(null);

  const selectedGroupId = useWatch({ control, name: 'selectedGroupId' });
  const name = useWatch({ control, name: 'name' });
  const description = useWatch({ control, name: 'description' });

  useEffect(() => {
    if (isGroupLocked && lockedGroup) {
      setProjectInfoValue('selectedGroupId', String(lockedGroup.id), DIRTY_VALIDATED_FIELD_OPTIONS);
      return;
    }

    const currentGroupId = getProjectInfoValues('selectedGroupId');
    const numericCurrentGroupId = Number(currentGroupId);
    const hasCurrentGroup = manageableGroups.some((group) => group.id === numericCurrentGroupId);
    if (currentGroupId.length > 0 && hasCurrentGroup) {
      return;
    }

    if (manageableGroups.length > 0) {
      setProjectInfoValue(
        'selectedGroupId',
        String(manageableGroups[0].id),
        DIRTY_VALIDATED_FIELD_OPTIONS,
      );
      return;
    }

    setProjectInfoValue('selectedGroupId', '', DIRTY_VALIDATED_FIELD_OPTIONS);
  }, [getProjectInfoValues, isGroupLocked, lockedGroup, manageableGroups, setProjectInfoValue]);

  const numericGroupId = Number(selectedGroupId);
  const isFinalizingProject =
    projectCreateMutation.isPending ||
    uploadDatasetMutation.isPending ||
    configureProjectSetupMutation.isPending ||
    assignProjectParticipantsMutation.isPending;

  const canCreateProject = isPositiveId(numericGroupId) && hasText(name) && isProjectInfoValid;

  const canUploadDataset = isPositiveId(numericGroupId) && selectedFiles.length > 0;

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
      void triggerProjectInfo();
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
        payload: toProjectPayload(getProjectInfoValues()),
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
    if (!canCreateProject || !canUploadDataset || !projectSetupPayload) {
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
    descriptionErrorMessage: projectInfoErrors.description?.message,
    finalizeProject,
    goToDatasetStep,
    goToInfoStep: () => setCurrentStep(1),
    goToSetupStep,
    isFinalizingProject,
    isGroupLocked,
    isLoadingGroups,
    manageableGroups,
    name,
    nameErrorMessage: projectInfoErrors.name?.message,
    numericGroupId,
    projectSetupPayload,
    rejectFiles,
    removeFile,
    selectFiles,
    selectedFiles,
    selectedGroupId,
    selectedGroupErrorMessage: projectInfoErrors.selectedGroupId?.message,
    setCurrentStep,
    setDescription: (nextDescription) =>
      setProjectInfoValue('description', nextDescription, DIRTY_VALIDATED_FIELD_OPTIONS),
    setName: (nextName) => setProjectInfoValue('name', nextName, DIRTY_VALIDATED_FIELD_OPTIONS),
    setSelectedGroupId: (nextGroupId) =>
      setProjectInfoValue('selectedGroupId', nextGroupId, DIRTY_VALIDATED_FIELD_OPTIONS),
    setupCompleted,
  };
}
