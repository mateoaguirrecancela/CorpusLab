import { act, renderHook, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { toast } from 'sonner';
import { useEditProjectDialog, type UseEditProjectDialogParams } from '@/modules/project/core/hooks/useEditProjectDialog';
import {
  useDeleteProjectMutation,
  useProjectAssignmentContextQuery,
  useUpdateProjectMutation,
} from '@/modules/project/shared/hooks/useProjectQueries';
import {
  getDeleteProjectErrorMessage,
  getProjectAssignmentContextErrorMessage,
  getUpdateProjectErrorMessage,
} from '@/modules/project/shared/services/projectService';
import { type ProjectAssignmentContext } from '@/modules/project/shared/types/project';

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (key: string) => key }),
}));

vi.mock('sonner', () => ({
  toast: { error: vi.fn(), success: vi.fn() },
}));

vi.mock('@/modules/project/shared/hooks/useProjectQueries', () => ({
  useDeleteProjectMutation: vi.fn(),
  useProjectAssignmentContextQuery: vi.fn(),
  useUpdateProjectMutation: vi.fn(),
}));

vi.mock('@/modules/project/shared/services/projectService', () => ({
  getDeleteProjectErrorMessage: vi.fn(() => 'delete-error'),
  getProjectAssignmentContextErrorMessage: vi.fn(() => 'assignment-context-error'),
  getUpdateProjectErrorMessage: vi.fn(() => 'update-error'),
}));

const assignmentContext: ProjectAssignmentContext = {
  researchGroupId: 1,
  currentUserId: 9,
  members: [
    { userId: 9, firstName: 'Creator', lastName: 'User', email: 'c@b.com' },
    { userId: 5, firstName: 'Jane', lastName: 'Doe', email: 'a@b.com' },
  ],
  defaultAssignments: [{ userId: 9, iaaGroup: 'GROUP_A' }],
};

const updateMutateAsync = vi.fn();
const deleteMutateAsync = vi.fn();

// A fresh object every call would change identity on every re-render (since
// renderHook re-invokes its callback), which retriggers the hook's
// close-resets-the-form effect and causes an infinite render loop. Build the
// params once per test and reuse the same reference.
function baseParams(overrides: Partial<UseEditProjectDialogParams> = {}): UseEditProjectDialogParams {
  return {
    groupId: 1,
    initialDescription: 'desc',
    initialName: 'Project',
    initialParticipantAssignments: [{ userId: 9, iaaGroup: 'GROUP_A' }],
    projectId: 10,
    ...overrides,
  };
}

beforeEach(() => {
  updateMutateAsync.mockReset().mockResolvedValue(undefined);
  deleteMutateAsync.mockReset().mockResolvedValue(undefined);
  vi.mocked(toast.error).mockClear();
  vi.mocked(toast.success).mockClear();
  vi.mocked(useUpdateProjectMutation).mockReturnValue({
    mutateAsync: updateMutateAsync,
    isPending: false,
  } as never);
  vi.mocked(useDeleteProjectMutation).mockReturnValue({
    mutateAsync: deleteMutateAsync,
    isPending: false,
  } as never);
  vi.mocked(useProjectAssignmentContextQuery).mockReturnValue({
    data: assignmentContext,
    isLoading: false,
    isError: false,
    error: null,
  } as never);
});

describe('useEditProjectDialog', () => {
  it('cannot save when nothing has changed', async () => {
    const params = baseParams();
    const { result } = renderHook(() => useEditProjectDialog(params));

    await waitFor(() => expect(result.current.canSave).toBe(false));
  });

  it('becomes savable once the name changes', async () => {
    const params = baseParams();
    const { result } = renderHook(() => useEditProjectDialog(params));
    await waitFor(() => expect(result.current.members).toHaveLength(2));

    act(() => result.current.setName('New name'));

    await waitFor(() => expect(result.current.canSave).toBe(true));
  });

  it('toggles a member through the assignment group sequence', async () => {
    const params = baseParams();
    const { result } = renderHook(() => useEditProjectDialog(params));
    await waitFor(() => expect(result.current.members).toHaveLength(2));

    act(() => result.current.toggleSelection(5));
    await waitFor(() => expect(result.current.assignmentByUserId[5]).toBe('GROUP_A'));

    expect(result.current.canSave).toBe(true);
  });

  it('updates the project and closes the dialog on success', async () => {
    const params = baseParams({ controlledOpen: true });
    const { result } = renderHook(() => useEditProjectDialog(params));
    await waitFor(() => expect(result.current.members).toHaveLength(2));
    act(() => result.current.setName('New name'));
    await waitFor(() => expect(result.current.canSave).toBe(true));

    await act(async () => {
      await result.current.handleUpdateProject();
    });

    expect(updateMutateAsync).toHaveBeenCalledWith(
      expect.objectContaining({
        groupId: 1,
        projectId: 10,
        payload: expect.objectContaining({ name: 'New name' }),
      }),
    );
    expect(toast.success).toHaveBeenCalledWith('project.edit.updateSuccess');
  });

  it('does not call the update mutation when it cannot save', async () => {
    const params = baseParams();
    const { result } = renderHook(() => useEditProjectDialog(params));
    await waitFor(() => expect(result.current.members).toHaveLength(2));

    await act(async () => {
      await result.current.handleUpdateProject();
    });

    expect(updateMutateAsync).not.toHaveBeenCalled();
  });

  it('shows an error toast when the update fails', async () => {
    updateMutateAsync.mockRejectedValue(new Error('boom'));
    const params = baseParams();
    const { result } = renderHook(() => useEditProjectDialog(params));
    await waitFor(() => expect(result.current.members).toHaveLength(2));
    act(() => result.current.setName('New name'));
    await waitFor(() => expect(result.current.canSave).toBe(true));

    await act(async () => {
      await result.current.handleUpdateProject();
    });

    expect(getUpdateProjectErrorMessage).toHaveBeenCalled();
    expect(toast.error).toHaveBeenCalledWith('update-error');
  });

  it('deletes the project, notifies onDeleted, and shows a success toast', async () => {
    const onDeleted = vi.fn();
    const params = baseParams({ onDeleted });
    const { result } = renderHook(() => useEditProjectDialog(params));

    await act(async () => {
      await result.current.handleDeleteProject();
    });

    expect(deleteMutateAsync).toHaveBeenCalledWith({ groupId: 1, projectId: 10 });
    expect(onDeleted).toHaveBeenCalled();
    expect(toast.success).toHaveBeenCalledWith('project.edit.deleteSuccess');
  });

  it('shows an error toast when deletion fails', async () => {
    deleteMutateAsync.mockRejectedValue(new Error('boom'));
    const params = baseParams();
    const { result } = renderHook(() => useEditProjectDialog(params));

    await act(async () => {
      await result.current.handleDeleteProject();
    });

    expect(getDeleteProjectErrorMessage).toHaveBeenCalled();
    expect(toast.error).toHaveBeenCalledWith('delete-error');
  });

  it('ignores a second delete request while one is already in flight', async () => {
    vi.mocked(useDeleteProjectMutation).mockReturnValue({
      mutateAsync: deleteMutateAsync,
      isPending: true,
    } as never);
    const params = baseParams();
    const { result } = renderHook(() => useEditProjectDialog(params));

    await act(async () => {
      await result.current.handleDeleteProject();
    });

    expect(deleteMutateAsync).not.toHaveBeenCalled();
  });

  it('resets the form and closes the confirm-delete dialog when closed', async () => {
    const params = baseParams();
    const { result } = renderHook(() => useEditProjectDialog(params));
    await waitFor(() => expect(result.current.members).toHaveLength(2));
    act(() => result.current.setName('New name'));
    await waitFor(() => expect(result.current.name).toBe('New name'));
    act(() => result.current.setConfirmDeleteOpen(true));

    act(() => result.current.handleOpenChange(false));

    await waitFor(() => expect(result.current.name).toBe('Project'));
    expect(result.current.confirmDeleteOpen).toBe(false);
  });

  it('defaults a null initial description to an empty string', () => {
    const params = baseParams({ initialDescription: null });
    const { result } = renderHook(() => useEditProjectDialog(params));

    expect(result.current.description).toBe('');
  });

  it('shows an error toast when the assignment context fails to load', () => {
    vi.mocked(useProjectAssignmentContextQuery).mockReturnValue({
      data: undefined,
      isLoading: false,
      isError: true,
      error: new Error('boom'),
    } as never);
    const params = baseParams();

    renderHook(() => useEditProjectDialog(params));

    expect(getProjectAssignmentContextErrorMessage).toHaveBeenCalled();
    expect(toast.error).toHaveBeenCalledWith('assignment-context-error', {
      id: 'edit-project-assignment-context-1',
    });
  });
});
