import { act, renderHook } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { toast } from 'sonner';
import { useProjectAssignmentStep } from '@/modules/project/participants/hooks/useProjectAssignmentStep';
import { useProjectAssignmentContextQuery } from '@/modules/project/shared/hooks/useProjectQueries';
import { getProjectAssignmentContextErrorMessage } from '@/modules/project/shared/services/projectService';
import { type ProjectAssignmentContext } from '@/modules/project/shared/types/project';

vi.mock('sonner', () => ({
  toast: { error: vi.fn() },
}));

vi.mock('@/modules/project/shared/hooks/useProjectQueries', () => ({
  useProjectAssignmentContextQuery: vi.fn(),
}));

vi.mock('@/modules/project/shared/services/projectService', () => ({
  getProjectAssignmentContextErrorMessage: vi.fn(() => 'assignment-context-error'),
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

beforeEach(() => {
  vi.mocked(toast.error).mockClear();
  vi.mocked(useProjectAssignmentContextQuery).mockReturnValue({
    data: assignmentContext,
    isLoading: false,
    isError: false,
    error: null,
  } as never);
});

describe('useProjectAssignmentStep', () => {
  it('starts from the server default assignments', () => {
    const { result } = renderHook(() =>
      useProjectAssignmentStep({ groupId: 1, onCompleted: vi.fn() }),
    );

    expect(result.current.assignmentByUserId).toEqual({ 9: 'GROUP_A' });
    expect(result.current.creatorUserId).toBe(9);
  });

  it('local toggles override the default assignment for that user', () => {
    const { result } = renderHook(() =>
      useProjectAssignmentStep({ groupId: 1, onCompleted: vi.fn() }),
    );

    act(() => result.current.toggleSelection(5));

    expect(result.current.assignmentByUserId).toEqual({ 9: 'GROUP_A', 5: 'GROUP_A' });
  });

  it('toggling the creator cycles only between GROUP_A and GROUP_B', () => {
    const { result } = renderHook(() =>
      useProjectAssignmentStep({ groupId: 1, onCompleted: vi.fn() }),
    );

    act(() => result.current.toggleSelection(9));

    expect(result.current.assignmentByUserId[9]).toBe('GROUP_B');
  });

  it('handleAssign forwards the resolved participant assignments', async () => {
    const onCompleted = vi.fn();
    const { result } = renderHook(() => useProjectAssignmentStep({ groupId: 1, onCompleted }));

    act(() => result.current.toggleSelection(5));
    await act(async () => {
      await result.current.handleAssign();
    });

    expect(onCompleted).toHaveBeenCalledWith(
      expect.arrayContaining([
        { userId: 9, iaaGroup: 'GROUP_A' },
        { userId: 5, iaaGroup: 'GROUP_A' },
      ]),
    );
  });

  it('shows an error toast when the assignment context fails to load', () => {
    vi.mocked(useProjectAssignmentContextQuery).mockReturnValue({
      data: undefined,
      isLoading: false,
      isError: true,
      error: new Error('boom'),
    } as never);

    renderHook(() => useProjectAssignmentStep({ groupId: 1, onCompleted: vi.fn() }));

    expect(getProjectAssignmentContextErrorMessage).toHaveBeenCalled();
    expect(toast.error).toHaveBeenCalledWith('assignment-context-error', {
      id: 'project-assignment-context-1',
    });
  });
});
