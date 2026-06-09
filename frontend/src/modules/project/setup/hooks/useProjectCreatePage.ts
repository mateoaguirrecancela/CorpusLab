import { useEffect, useMemo, useState } from 'react';
import { zodResolver } from '@hookform/resolvers/zod';
import { useTranslation } from 'react-i18next';
import { type FileRejection } from 'react-dropzone';
import { useForm, useWatch } from 'react-hook-form';
import { useNavigate, useSearchParams } from 'react-router';
import { toast } from 'sonner';
import {
  type ConfigureProjectSetupPayload,
  type ProjectParticipantAssignment,
} from '@/modules/project/shared/types/project';
import { useFinalizeProjectWizard } from '@/modules/project/setup/hooks/useFinalizeProjectWizard';
import {
  createProjectInfoSchema,
  type ProjectCreateInfoFormValues,
} from '@/modules/project/shared/schemas/projectFormSchemas';
import { validateProjectFiles } from '@/modules/project/setup/utils/projectCreateValidation';
import {
  DIRTY_VALIDATED_FIELD_OPTIONS,
  hasText,
  isPositiveId,
  toProjectPayload,
} from '@/modules/project/shared/utils/projectFormUtils';
import { useProjectSetupResearchGroupsQuery } from '@/modules/project/setup/hooks/useProjectSetupResearchGroupsQuery';
import { type ProjectSetupResearchGroupSummary } from '@/modules/project/setup/types/projectSetup';

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
  manageableGroups: ProjectSetupResearchGroupSummary[];
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

  const { data: groups = [], isLoading: isLoadingGroups } = useProjectSetupResearchGroupsQuery();
  const { finalizeProjectWizard, isFinalizingProject, resetFinalizationProject } =
    useFinalizeProjectWizard();
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

    resetFinalizationProject();
    setCurrentStep(2);
  };

  const goToSetupStep = () => {
    if (!canUploadDataset) {
      return;
    }

    resetFinalizationProject();
    setProjectSetupPayload(null);
    setCurrentStep(3);
  };

  const setupCompleted = async (payload: ConfigureProjectSetupPayload) => {
    setProjectSetupPayload(payload);
    setCurrentStep(4);
  };

  const finalizeProject = async (participantAssignments: ProjectParticipantAssignment[]) => {
    if (!canCreateProject || !canUploadDataset || !projectSetupPayload) {
      return;
    }

    const result = await finalizeProjectWizard({
      files: selectedFiles,
      groupId: numericGroupId,
      participantAssignments,
      projectInfo: toProjectPayload(getProjectInfoValues()),
      setupPayload: projectSetupPayload,
    });

    if (!result.ok) {
      return;
    }

    if (isGroupLocked) {
      navigate(`/home/research-groups/${numericGroupId}`);
      return;
    }

    navigate(`/home/projects/${result.projectId}`);
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
