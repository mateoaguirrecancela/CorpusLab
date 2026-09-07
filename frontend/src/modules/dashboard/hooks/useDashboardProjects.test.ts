import { renderHook } from '@testing-library/react';
import { useQuery } from '@tanstack/react-query';
import { describe, expect, it, vi } from 'vitest';
import { useDashboardProjects } from '@/modules/dashboard/hooks/useDashboardProjects';
import { getDashboardProjectsErrorMessage } from '@/modules/dashboard/services/dashboardService';
import { type DashboardProjectsResponse } from '@/modules/dashboard/types/dashboard';

const navigateMock = vi.fn();

vi.mock('@tanstack/react-query', () => ({
  useQuery: vi.fn(),
}));

vi.mock('react-router', () => ({
  useNavigate: () => navigateMock,
}));

vi.mock('@/modules/dashboard/services/dashboardService', () => ({
  getDashboardProjects: vi.fn(),
  getDashboardProjectsErrorMessage: vi.fn(() => 'dashboard-projects-error'),
}));

function mockQuery(overrides: Partial<{ data: DashboardProjectsResponse; isLoading: boolean; isError: boolean; error: unknown }>) {
  vi.mocked(useQuery).mockReturnValue({
    data: undefined,
    isLoading: false,
    isError: false,
    error: null,
    ...overrides,
  } as never);
}

describe('useDashboardProjects', () => {
  it('defaults to empty project lists while there is no data', () => {
    mockQuery({});

    const { result } = renderHook(() => useDashboardProjects());

    expect(result.current.advancedProjects).toEqual([]);
    expect(result.current.recentProjects).toEqual([]);
  });

  it('exposes the projects from the query response', () => {
    const data: DashboardProjectsResponse = {
      advancedProjects: [{ id: 1 } as never],
      recentProjects: [{ id: 2 } as never],
    };
    mockQuery({ data });

    const { result } = renderHook(() => useDashboardProjects());

    expect(result.current.advancedProjects).toEqual(data.advancedProjects);
    expect(result.current.recentProjects).toEqual(data.recentProjects);
  });

  it('navigates to the right route for each action', () => {
    mockQuery({});
    const { result } = renderHook(() => useDashboardProjects());

    result.current.createProject();
    expect(navigateMock).toHaveBeenCalledWith('/home/projects/create');

    result.current.openProjects();
    expect(navigateMock).toHaveBeenCalledWith('/home/projects');

    result.current.openProject(42);
    expect(navigateMock).toHaveBeenCalledWith('/home/projects/42');
  });

  it('maps a query error to the dashboard projects error message', () => {
    mockQuery({ isError: true, error: new Error('boom') });

    const { result } = renderHook(() => useDashboardProjects());

    expect(getDashboardProjectsErrorMessage).toHaveBeenCalled();
    expect(result.current.errorMessage).toBe('dashboard-projects-error');
  });
});
