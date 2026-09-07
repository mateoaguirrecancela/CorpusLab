import { act, renderHook } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { toast } from 'sonner';
import { useFinalizeProjectWizard } from '@/modules/project/setup/hooks/useFinalizeProjectWizard';
import {
  type ConfigureProjectSetupPayload,
  type CreateProjectPayload,
} from '@/modules/project/shared/types/project';

const createMutateAsync = vi.fn();
const uploadMutateAsync = vi.fn();
const configureMutateAsync = vi.fn();
const assignMutateAsync = vi.fn();
const cleanupIncompleteProject = vi.fn();

vi.mock('sonner', () => ({
  toast: { error: vi.fn() },
}));

vi.mock('@/modules/project/shared/services/projectService', () => ({
  cleanupIncompleteProject: (...args: unknown[]) => cleanupIncompleteProject(...args),
  getAssignParticipantsErrorMessage: () => 'assign-error',
  getCreateProjectErrorMessage: () => 'create-error',
  getProjectSetupErrorMessage: () => 'setup-error',
  getUploadDatasetErrorMessage: () => 'upload-error',
}));

vi.mock('@/modules/project/shared/hooks/useProjectQueries', () => ({
  useProjectCreateMutation: () => ({ mutateAsync: createMutateAsync, isPending: false }),
  useUploadProjectDatasetMutation: () => ({ mutateAsync: uploadMutateAsync, isPending: false }),
  useConfigureProjectSetupMutation: () => ({ mutateAsync: configureMutateAsync, isPending: false }),
  useAssignProjectParticipantsMutation: () => ({ mutateAsync: assignMutateAsync, isPending: false }),
}));

const projectInfo: CreateProjectPayload = { name: 'Project', description: undefined };
const setupPayload: ConfigureProjectSetupPayload = {
  projectType: 'TEXT_CLASSIFICATION_SIMPLE',
  labels: [],
};

function baseInput() {
  return {
    files: [],
    groupId: 1,
    participantAssignments: [],
    projectInfo,
    setupPayload,
  };
}

beforeEach(() => {
  createMutateAsync.mockReset().mockResolvedValue({ id: 100 });
  uploadMutateAsync.mockReset().mockResolvedValue(undefined);
  configureMutateAsync.mockReset().mockResolvedValue(undefined);
  assignMutateAsync.mockReset().mockResolvedValue(undefined);
  cleanupIncompleteProject.mockReset().mockResolvedValue(undefined);
  vi.mocked(toast.error).mockClear();
});

describe('useFinalizeProjectWizard', () => {
  it('runs create, upload, configure and assign in order and reports the new project id', async () => {
    const { result } = renderHook(() => useFinalizeProjectWizard());

    let outcome;
    await act(async () => {
      outcome = await result.current.finalizeProjectWizard(baseInput());
    });

    expect(outcome).toEqual({ ok: true, projectId: 100 });
    expect(createMutateAsync).toHaveBeenCalledWith({ groupId: 1, payload: projectInfo });
    expect(uploadMutateAsync).toHaveBeenCalledWith({ groupId: 1, projectId: 100, files: [] });
    expect(configureMutateAsync).toHaveBeenCalledWith({
      groupId: 1,
      projectId: 100,
      payload: setupPayload,
    });
    expect(assignMutateAsync).toHaveBeenCalledWith({
      groupId: 1,
      projectId: 100,
      payload: { participantAssignments: [] },
    });
    expect(cleanupIncompleteProject).not.toHaveBeenCalled();
  });

  it('stops and reports failure without cleanup when project creation itself fails', async () => {
    createMutateAsync.mockRejectedValue(new Error('boom'));
    const { result } = renderHook(() => useFinalizeProjectWizard());

    let outcome;
    await act(async () => {
      outcome = await result.current.finalizeProjectWizard(baseInput());
    });

    expect(outcome).toEqual({ ok: false });
    expect(toast.error).toHaveBeenCalledWith('create-error');
    expect(uploadMutateAsync).not.toHaveBeenCalled();
    expect(cleanupIncompleteProject).not.toHaveBeenCalled();
  });

  it('cleans up the created project when the dataset upload fails', async () => {
    uploadMutateAsync.mockRejectedValue(new Error('boom'));
    const { result } = renderHook(() => useFinalizeProjectWizard());

    let outcome;
    await act(async () => {
      outcome = await result.current.finalizeProjectWizard(baseInput());
    });

    expect(outcome).toEqual({ ok: false });
    expect(toast.error).toHaveBeenCalledWith('upload-error');
    expect(configureMutateAsync).not.toHaveBeenCalled();
    expect(cleanupIncompleteProject).toHaveBeenCalledWith(1, 100);
  });

  it('cleans up when setup configuration fails', async () => {
    configureMutateAsync.mockRejectedValue(new Error('boom'));
    const { result } = renderHook(() => useFinalizeProjectWizard());

    let outcome;
    await act(async () => {
      outcome = await result.current.finalizeProjectWizard(baseInput());
    });

    expect(outcome).toEqual({ ok: false });
    expect(toast.error).toHaveBeenCalledWith('setup-error');
    expect(assignMutateAsync).not.toHaveBeenCalled();
    expect(cleanupIncompleteProject).toHaveBeenCalledWith(1, 100);
  });

  it('cleans up when participant assignment fails', async () => {
    assignMutateAsync.mockRejectedValue(new Error('boom'));
    const { result } = renderHook(() => useFinalizeProjectWizard());

    let outcome;
    await act(async () => {
      outcome = await result.current.finalizeProjectWizard(baseInput());
    });

    expect(outcome).toEqual({ ok: false });
    expect(toast.error).toHaveBeenCalledWith('assign-error');
    expect(cleanupIncompleteProject).toHaveBeenCalledWith(1, 100);
  });

  it('creates a fresh project on every retry, since a failed attempt is fully cleaned up', async () => {
    uploadMutateAsync.mockRejectedValueOnce(new Error('boom'));
    const { result } = renderHook(() => useFinalizeProjectWizard());

    await act(async () => {
      await result.current.finalizeProjectWizard(baseInput());
    });
    expect(createMutateAsync).toHaveBeenCalledTimes(1);
    expect(cleanupIncompleteProject).toHaveBeenCalledTimes(1);

    let outcome;
    await act(async () => {
      outcome = await result.current.finalizeProjectWizard(baseInput());
    });

    expect(outcome).toEqual({ ok: true, projectId: 100 });
    expect(createMutateAsync).toHaveBeenCalledTimes(2);
  });
});
