import { act, renderHook } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { toast } from 'sonner';
import { useProjectsPage } from '@/modules/project/core/hooks/useProjectsPage';
import { useMyAssignedProjectsQuery } from '@/modules/project/shared/hooks/useProjectQueries';
import { getProjectsLoadErrorMessage } from '@/modules/project/shared/services/projectService';

const navigateMock = vi.fn();
const fetchNextPage = vi.fn();

vi.mock('react-router', () => ({
  useNavigate: () => navigateMock,
}));

vi.mock('sonner', () => ({
  toast: { error: vi.fn() },
}));

vi.mock('@/modules/project/shared/hooks/useProjectQueries', () => ({
  useMyAssignedProjectsQuery: vi.fn(),
}));

vi.mock('@/modules/project/shared/services/projectService', () => ({
  getProjectsLoadErrorMessage: vi.fn(() => 'projects-load-error'),
}));

function mockQuery(overrides: Record<string, unknown> = {}) {
  vi.mocked(useMyAssignedProjectsQuery).mockReturnValue({
    data: undefined,
    isLoading: false,
    isError: false,
    error: null,
    hasNextPage: false,
    fetchNextPage,
    isFetchingNextPage: false,
    ...overrides,
  } as never);
}

beforeEach(() => {
  navigateMock.mockClear();
  fetchNextPage.mockClear();
  vi.mocked(toast.error).mockClear();
});

describe('useProjectsPage', () => {
  it('flattens the paginated project pages', () => {
    mockQuery({
      data: {
        pages: [
          { content: [{ id: 1 }] },
          { content: [{ id: 2 }] },
        ],
      },
    });

    const { result } = renderHook(() => useProjectsPage());

    expect(result.current.projects).toEqual([{ id: 1 }, { id: 2 }]);
  });

  it('toggles the archived filter', () => {
    mockQuery({});
    const { result } = renderHook(() => useProjectsPage());

    expect(result.current.showArchived).toBe(false);
    act(() => result.current.toggleArchived());
    expect(result.current.showArchived).toBe(true);
  });

  it('navigates to the right route for each action', () => {
    mockQuery({});
    const { result } = renderHook(() => useProjectsPage());

    result.current.openCreateProject();
    expect(navigateMock).toHaveBeenCalledWith('/home/projects/create');

    result.current.openProject(7);
    expect(navigateMock).toHaveBeenCalledWith('/home/projects/7');
  });

  it('delegates loadMoreProjects to fetchNextPage', () => {
    mockQuery({});
    const { result } = renderHook(() => useProjectsPage());

    result.current.loadMoreProjects();

    expect(fetchNextPage).toHaveBeenCalled();
  });

  it('shows a load-error toast when the query fails', () => {
    mockQuery({ isError: true, error: new Error('boom') });

    renderHook(() => useProjectsPage());

    expect(getProjectsLoadErrorMessage).toHaveBeenCalled();
    expect(toast.error).toHaveBeenCalledWith('projects-load-error', { id: 'projects-load-error' });
  });
});
