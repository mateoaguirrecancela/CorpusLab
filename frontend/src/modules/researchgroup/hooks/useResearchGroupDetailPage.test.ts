import { act, renderHook } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { useParams } from 'react-router';
import { useResearchGroupDetailPage } from '@/modules/researchgroup/hooks/useResearchGroupDetailPage';
import { useResearchGroupDetailQuery } from '@/modules/researchgroup/hooks/useResearchGroupQueries';
import { useResearchGroupAssignedProjectsQuery } from '@/modules/researchgroup/hooks/useResearchGroupProjectQueries';
import { getResearchGroupDetailErrorMessage } from '@/modules/researchgroup/services/researchGroupService';
import { useResearchGroupUIStore } from '@/modules/researchgroup/stores/useResearchGroupUIStore';
import { type ResearchGroupDetail } from '@/modules/researchgroup/types/researchGroup';

const navigateMock = vi.fn();
const fetchNextProjectsPage = vi.fn();

vi.mock('react-i18next', () => {
  const t = (key: string) => key;
  return { useTranslation: () => ({ t }) };
});

vi.mock('react-router', () => ({
  useNavigate: () => navigateMock,
  useParams: vi.fn(),
}));

vi.mock('@/modules/researchgroup/hooks/useResearchGroupQueries', () => ({
  useResearchGroupDetailQuery: vi.fn(),
}));

vi.mock('@/modules/researchgroup/hooks/useResearchGroupProjectQueries', () => ({
  useResearchGroupAssignedProjectsQuery: vi.fn(),
}));

vi.mock('@/modules/researchgroup/services/researchGroupService', () => ({
  getResearchGroupDetailErrorMessage: vi.fn(() => 'detail-error'),
}));

vi.mock('@/modules/researchgroup/services/researchGroupProjectService', () => ({
  getResearchGroupProjectsLoadErrorMessage: vi.fn(() => 'projects-error'),
}));

const group: ResearchGroupDetail = {
  id: 1,
  name: 'Group',
  description: null,
  invitationCode: 'ABC',
  totalMembers: 2,
  activeProjects: 1,
  createdAt: '2026-01-01T00:00:00Z',
  members: [],
  canManageResearchers: true,
  canCreateProjects: true,
};

function mockDetailQuery(overrides: Record<string, unknown> = {}) {
  vi.mocked(useResearchGroupDetailQuery).mockReturnValue({
    data: group,
    isLoading: false,
    isError: false,
    error: null,
    ...overrides,
  } as never);
}

function mockProjectsQuery(overrides: Record<string, unknown> = {}) {
  vi.mocked(useResearchGroupAssignedProjectsQuery).mockReturnValue({
    data: undefined,
    isLoading: false,
    isError: false,
    error: null,
    hasNextPage: false,
    fetchNextPage: fetchNextProjectsPage,
    isFetchingNextPage: false,
    ...overrides,
  } as never);
}

beforeEach(() => {
  navigateMock.mockClear();
  fetchNextProjectsPage.mockClear();
  useResearchGroupUIStore.setState({ archivedProjectsByGroupId: {} });
  vi.mocked(useParams).mockReturnValue({ id: '1' } as never);
  mockDetailQuery();
  mockProjectsQuery();
});

describe('useResearchGroupDetailPage', () => {
  it('exposes the group permissions', () => {
    const { result } = renderHook(() => useResearchGroupDetailPage());

    expect(result.current.canCreateProjects).toBe(true);
    expect(result.current.canManageResearchers).toBe(true);
  });

  it('reports an invalid-id error for a non-numeric route param', () => {
    vi.mocked(useParams).mockReturnValue({ id: 'abc' } as never);
    mockDetailQuery({ data: undefined });

    const { result } = renderHook(() => useResearchGroupDetailPage());

    expect(result.current.errorMessage).toBe('researchGroup.errors.invalidGroupId');
    expect(getResearchGroupDetailErrorMessage).not.toHaveBeenCalled();
  });

  it('flattens assigned project pages', () => {
    mockProjectsQuery({ data: { pages: [{ content: [{ id: 5 }] }] } });
    const { result } = renderHook(() => useResearchGroupDetailPage());

    expect(result.current.assignedProjects).toEqual([{ id: 5 }]);
  });

  it('maps a group detail query error', () => {
    mockDetailQuery({ data: undefined, isError: true, error: new Error('boom') });
    const { result } = renderHook(() => useResearchGroupDetailPage());

    expect(getResearchGroupDetailErrorMessage).toHaveBeenCalled();
    expect(result.current.errorMessage).toBe('detail-error');
  });

  it('toggles the archived-projects flag per group in the shared store', () => {
    const { result } = renderHook(() => useResearchGroupDetailPage());

    expect(result.current.showArchivedProjects).toBe(false);
    act(() => result.current.toggleArchivedProjects());
    expect(result.current.showArchivedProjects).toBe(true);
  });

  it('navigates to create a project scoped to the current group', () => {
    const { result } = renderHook(() => useResearchGroupDetailPage());

    result.current.createProject();

    expect(navigateMock).toHaveBeenCalledWith('/home/projects/create?groupId=1');
  });

  it('does not navigate to create a project while the group has not loaded', () => {
    mockDetailQuery({ data: undefined });
    const { result } = renderHook(() => useResearchGroupDetailPage());

    result.current.createProject();

    expect(navigateMock).not.toHaveBeenCalled();
  });

  it('delegates loadMoreProjects to fetchNextPage', () => {
    const { result } = renderHook(() => useResearchGroupDetailPage());

    result.current.loadMoreProjects();

    expect(fetchNextProjectsPage).toHaveBeenCalled();
  });
});
