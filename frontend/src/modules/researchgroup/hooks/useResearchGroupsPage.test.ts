import { renderHook } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { toast } from 'sonner';
import { useResearchGroupsPage } from '@/modules/researchgroup/hooks/useResearchGroupsPage';
import {
  useResearchGroupInvitationsQuery,
  useResearchGroupsQuery,
} from '@/modules/researchgroup/hooks/useResearchGroupQueries';
import { getResearchGroupsErrorMessage } from '@/modules/researchgroup/services/researchGroupService';

const navigateMock = vi.fn();

vi.mock('react-router', () => ({
  useNavigate: () => navigateMock,
}));

vi.mock('sonner', () => ({
  toast: { error: vi.fn() },
}));

vi.mock('@/modules/researchgroup/hooks/useResearchGroupQueries', () => ({
  useResearchGroupInvitationsQuery: vi.fn(),
  useResearchGroupsQuery: vi.fn(),
}));

vi.mock('@/modules/researchgroup/services/researchGroupService', () => ({
  getResearchGroupsErrorMessage: vi.fn(() => 'groups-error'),
}));

beforeEach(() => {
  navigateMock.mockClear();
  vi.mocked(toast.error).mockClear();
  vi.mocked(useResearchGroupInvitationsQuery).mockReturnValue({ data: [] } as never);
  vi.mocked(useResearchGroupsQuery).mockReturnValue({
    data: [],
    isLoading: false,
    isError: false,
    error: null,
  } as never);
});

describe('useResearchGroupsPage', () => {
  it('exposes the loaded groups and invitations', () => {
    vi.mocked(useResearchGroupsQuery).mockReturnValue({
      data: [{ id: 1 }],
      isLoading: false,
      isError: false,
      error: null,
    } as never);
    vi.mocked(useResearchGroupInvitationsQuery).mockReturnValue({ data: [{ id: 2 }] } as never);

    const { result } = renderHook(() => useResearchGroupsPage());

    expect(result.current.groups).toEqual([{ id: 1 }]);
    expect(result.current.invitations).toEqual([{ id: 2 }]);
  });

  it('navigates to the selected research group', () => {
    const { result } = renderHook(() => useResearchGroupsPage());

    result.current.openResearchGroup(7);

    expect(navigateMock).toHaveBeenCalledWith('/home/research-groups/7');
  });

  it('shows a load-error toast when the groups query fails', () => {
    vi.mocked(useResearchGroupsQuery).mockReturnValue({
      data: undefined,
      isLoading: false,
      isError: true,
      error: new Error('boom'),
    } as never);

    renderHook(() => useResearchGroupsPage());

    expect(getResearchGroupsErrorMessage).toHaveBeenCalled();
    expect(toast.error).toHaveBeenCalledWith('groups-error', { id: 'research-groups-load-error' });
  });
});
