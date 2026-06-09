import { useState } from 'react';
import { toast } from 'sonner';
import {
  useAssignProjectParticipantsMutation,
  useConfigureProjectSetupMutation,
  useProjectCreateMutation,
  useUploadProjectDatasetMutation,
} from '@/modules/project/shared/hooks/useProjectQueries';
import {
  cleanupIncompleteProject,
  getAssignParticipantsErrorMessage,
  getCreateProjectErrorMessage,
  getProjectSetupErrorMessage,
  getUploadDatasetErrorMessage,
} from '@/modules/project/shared/services/projectService';
import type {
  ConfigureProjectSetupPayload,
  CreateProjectPayload,
  ProjectParticipantAssignment,
} from '@/modules/project/shared/types/project';

type FinalizeProjectWizardInput = Readonly<{
  files: File[];
  groupId: number;
  participantAssignments: ProjectParticipantAssignment[];
  projectInfo: CreateProjectPayload;
  setupPayload: ConfigureProjectSetupPayload;
}>;

type FinalizeProjectWizardResult =
  | Readonly<{
      ok: true;
      projectId: number;
    }>
  | Readonly<{
      ok: false;
    }>;

type FinalizeProjectWizardState = Readonly<{
  finalizeProjectWizard: (
    input: FinalizeProjectWizardInput,
  ) => Promise<FinalizeProjectWizardResult>;
  isFinalizingProject: boolean;
  resetFinalizationProject: () => void;
}>;

type WizardFailureStep = 'dataset_upload_failed' | 'setup_config_failed' | 'assignment_failed';

export function useFinalizeProjectWizard(): FinalizeProjectWizardState {
  const projectCreateMutation = useProjectCreateMutation();
  const uploadDatasetMutation = useUploadProjectDatasetMutation();
  const configureProjectSetupMutation = useConfigureProjectSetupMutation();
  const assignProjectParticipantsMutation = useAssignProjectParticipantsMutation();
  const [finalizationProjectId, setFinalizationProjectId] = useState<number | null>(null);

  const ensureProjectCreated = async ({
    groupId,
    projectInfo,
  }: Pick<FinalizeProjectWizardInput, 'groupId' | 'projectInfo'>): Promise<number | null> => {
    if (finalizationProjectId !== null) {
      return finalizationProjectId;
    }

    try {
      const createdProject = await projectCreateMutation.mutateAsync({
        groupId,
        payload: projectInfo,
      });

      setFinalizationProjectId(createdProject.id);
      return createdProject.id;
    } catch (error) {
      toast.error(getCreateProjectErrorMessage(error));
      return null;
    }
  };

  const uploadDataset = async ({
    files,
    groupId,
    projectId,
  }: Pick<FinalizeProjectWizardInput, 'files' | 'groupId'> & {
    projectId: number;
  }): Promise<boolean> => {
    try {
      await uploadDatasetMutation.mutateAsync({
        groupId,
        projectId,
        files,
      });
      return true;
    } catch (error) {
      toast.error(getUploadDatasetErrorMessage(error));
      return false;
    }
  };

  const configureSetup = async ({
    groupId,
    projectId,
    setupPayload,
  }: Pick<FinalizeProjectWizardInput, 'groupId' | 'setupPayload'> & {
    projectId: number;
  }): Promise<boolean> => {
    try {
      await configureProjectSetupMutation.mutateAsync({
        groupId,
        projectId,
        payload: setupPayload,
      });
      return true;
    } catch (error) {
      toast.error(getProjectSetupErrorMessage(error));
      return false;
    }
  };

  const assignParticipants = async ({
    groupId,
    participantAssignments,
    projectId,
  }: Pick<FinalizeProjectWizardInput, 'groupId' | 'participantAssignments'> & {
    projectId: number;
  }): Promise<boolean> => {
    try {
      await assignProjectParticipantsMutation.mutateAsync({
        groupId,
        projectId,
        payload: { participantAssignments },
      });
      return true;
    } catch (error) {
      toast.error(getAssignParticipantsErrorMessage(error));
      return false;
    }
  };

  const cleanupFailedProject = async (groupId: number, projectId: number) => {
    setFinalizationProjectId(null);
    try {
      await cleanupIncompleteProject(groupId, projectId);
    } catch (deleteError) {
      console.error('Failed to cleanup project after wizard error', deleteError);
    }
  };

  const finalizeProjectWizard = async (
    input: FinalizeProjectWizardInput,
  ): Promise<FinalizeProjectWizardResult> => {
    const projectId = await ensureProjectCreated(input);
    if (projectId === null) {
      return { ok: false };
    }

    try {
      const uploadedDataset = await uploadDataset({ ...input, projectId });
      if (!uploadedDataset) {
        throw new Error('dataset_upload_failed' satisfies WizardFailureStep);
      }

      const configuredSetup = await configureSetup({ ...input, projectId });
      if (!configuredSetup) {
        throw new Error('setup_config_failed' satisfies WizardFailureStep);
      }

      const assignedParticipants = await assignParticipants({ ...input, projectId });
      if (!assignedParticipants) {
        throw new Error('assignment_failed' satisfies WizardFailureStep);
      }

      return { ok: true, projectId };
    } catch (error) {
      const failureStep = (error as Error).message as WizardFailureStep;
      if (
        failureStep === 'dataset_upload_failed' ||
        failureStep === 'setup_config_failed' ||
        failureStep === 'assignment_failed'
      ) {
        await cleanupFailedProject(input.groupId, projectId);
      }

      return { ok: false };
    }
  };

  return {
    finalizeProjectWizard,
    isFinalizingProject:
      projectCreateMutation.isPending ||
      uploadDatasetMutation.isPending ||
      configureProjectSetupMutation.isPending ||
      assignProjectParticipantsMutation.isPending,
    resetFinalizationProject: () => setFinalizationProjectId(null),
  };
}
