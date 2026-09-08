import { renderHook } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { toast } from 'sonner';
import { useAnnotationWorkspace } from '@/modules/project/annotation/hooks/useAnnotationWorkspace';
import {
  useProjectAnnotationWorkspaceQuery,
  useProjectDetailQuery,
  useProjectParticipantAnnotationWorkspaceQuery,
} from '@/modules/project/shared/hooks/useProjectQueries';
import {
  getProjectAnnotationLoadErrorMessage,
  getProjectDetailLoadErrorMessage,
} from '@/modules/project/shared/services/projectService';
import { type ProjectAnnotationWorkspace, type ProjectDetail } from '@/modules/project/shared/types/project';

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (key: string) => key }),
}));

vi.mock('sonner', () => ({
  toast: { error: vi.fn() },
}));

vi.mock('@/modules/project/shared/hooks/useProjectQueries', () => ({
  useProjectAnnotationWorkspaceQuery: vi.fn(),
  useProjectDetailQuery: vi.fn(),
  useProjectParticipantAnnotationWorkspaceQuery: vi.fn(),
}));

vi.mock('@/modules/project/shared/services/projectService', () => ({
  getProjectAnnotationLoadErrorMessage: vi.fn(() => 'workspace-load-error'),
  getProjectDetailLoadErrorMessage: vi.fn(() => 'detail-load-error'),
}));

const project: ProjectDetail = {
  id: 1,
  name: 'Project',
  projectType: 'TEXT_CLASSIFICATION_SIMPLE',
  labels: [{ name: 'positive', color: null }],
  annotationTargetColumn: null,
  participants: [
    { userId: 5, firstName: 'Jane', lastName: 'Doe', email: 'a@b.com', role: 'PARTICIPANT', iaaGroup: 'GROUP_A', completionPercentage: 0 },
  ],
} as unknown as ProjectDetail;

const workspace: ProjectAnnotationWorkspace = {
  projectId: 1,
  projectType: 'NER',
  annotationTargetColumn: 'text',
  labels: [{ name: 'PERSON', color: '#111' }],
  offset: 0,
  limit: 50,
  totalSteps: 3,
  completedSteps: 0,
  completionPercentage: 0,
  firstPendingStepIndex: 1,
  steps: [],
};

function idleQuery(overrides: Partial<{ data: unknown; isLoading: boolean; isError: boolean; error: unknown }> = {}) {
  return { data: undefined, isLoading: false, isError: false, error: null, ...overrides };
}

beforeEach(() => {
  vi.mocked(toast.error).mockClear();
});

describe('useAnnotationWorkspace', () => {
  it('uses the own-workspace query outside review mode', () => {
    vi.mocked(useProjectDetailQuery).mockReturnValue(idleQuery({ data: project }) as never);
    vi.mocked(useProjectAnnotationWorkspaceQuery).mockReturnValue(idleQuery({ data: workspace }) as never);
    vi.mocked(useProjectParticipantAnnotationWorkspaceQuery).mockReturnValue(idleQuery() as never);

    const { result } = renderHook(() =>
      useAnnotationWorkspace({
        annotationOffset: 0,
        isInvalidProjectId: false,
        isReviewMode: false,
        numericProjectId: 1,
        reviewedParticipantUserId: null,
      }),
    );

    expect(result.current.annotationWorkspace).toEqual(workspace);
    expect(result.current.annotationProjectType).toBe('NER');
    expect(result.current.totalSteps).toBe(3);
  });

  it('uses the participant workspace query in review mode', () => {
    vi.mocked(useProjectDetailQuery).mockReturnValue(idleQuery({ data: project }) as never);
    vi.mocked(useProjectAnnotationWorkspaceQuery).mockReturnValue(idleQuery() as never);
    vi.mocked(useProjectParticipantAnnotationWorkspaceQuery).mockReturnValue(
      idleQuery({ data: workspace }) as never,
    );

    const { result } = renderHook(() =>
      useAnnotationWorkspace({
        annotationOffset: 0,
        isInvalidProjectId: false,
        isReviewMode: true,
        numericProjectId: 1,
        reviewedParticipantUserId: 5,
      }),
    );

    expect(result.current.annotationWorkspace).toEqual(workspace);
    expect(result.current.reviewedParticipant?.userId).toBe(5);
  });

  it('is null for reviewedParticipant when the user is not among the participants', () => {
    vi.mocked(useProjectDetailQuery).mockReturnValue(idleQuery({ data: project }) as never);
    vi.mocked(useProjectAnnotationWorkspaceQuery).mockReturnValue(idleQuery() as never);
    vi.mocked(useProjectParticipantAnnotationWorkspaceQuery).mockReturnValue(idleQuery() as never);

    const { result } = renderHook(() =>
      useAnnotationWorkspace({
        annotationOffset: 0,
        isInvalidProjectId: false,
        isReviewMode: true,
        numericProjectId: 1,
        reviewedParticipantUserId: 999,
      }),
    );

    expect(result.current.reviewedParticipant).toBeNull();
  });

  it('reports an invalid-id error without calling the project detail service', () => {
    vi.mocked(useProjectDetailQuery).mockReturnValue(idleQuery() as never);
    vi.mocked(useProjectAnnotationWorkspaceQuery).mockReturnValue(idleQuery() as never);
    vi.mocked(useProjectParticipantAnnotationWorkspaceQuery).mockReturnValue(idleQuery() as never);

    const { result } = renderHook(() =>
      useAnnotationWorkspace({
        annotationOffset: 0,
        isInvalidProjectId: true,
        isReviewMode: false,
        numericProjectId: -1,
        reviewedParticipantUserId: null,
      }),
    );

    expect(result.current.detailErrorMessage).toBe('project.annotationPage.errors.invalidId');
    expect(getProjectDetailLoadErrorMessage).not.toHaveBeenCalled();
    expect(toast.error).toHaveBeenCalledWith('project.annotationPage.errors.invalidId', {
      id: 'project-annotation-detail-load-error',
    });
  });

  it('maps a project detail query error to the mapped error message', () => {
    vi.mocked(useProjectDetailQuery).mockReturnValue(
      idleQuery({ isError: true, error: new Error('boom') }) as never,
    );
    vi.mocked(useProjectAnnotationWorkspaceQuery).mockReturnValue(idleQuery() as never);
    vi.mocked(useProjectParticipantAnnotationWorkspaceQuery).mockReturnValue(idleQuery() as never);

    const { result } = renderHook(() =>
      useAnnotationWorkspace({
        annotationOffset: 0,
        isInvalidProjectId: false,
        isReviewMode: false,
        numericProjectId: 1,
        reviewedParticipantUserId: null,
      }),
    );

    expect(getProjectDetailLoadErrorMessage).toHaveBeenCalled();
    expect(result.current.detailErrorMessage).toBe('detail-load-error');
  });

  it('maps a workspace query error to the mapped error message', () => {
    vi.mocked(useProjectDetailQuery).mockReturnValue(idleQuery({ data: project }) as never);
    vi.mocked(useProjectAnnotationWorkspaceQuery).mockReturnValue(
      idleQuery({ isError: true, error: new Error('boom') }) as never,
    );
    vi.mocked(useProjectParticipantAnnotationWorkspaceQuery).mockReturnValue(idleQuery() as never);

    renderHook(() =>
      useAnnotationWorkspace({
        annotationOffset: 0,
        isInvalidProjectId: false,
        isReviewMode: false,
        numericProjectId: 1,
        reviewedParticipantUserId: null,
      }),
    );

    expect(getProjectAnnotationLoadErrorMessage).toHaveBeenCalled();
    expect(toast.error).toHaveBeenCalledWith('workspace-load-error', {
      id: 'project-annotation-workspace-load-error',
    });
  });

  it('is not ready to render while the project or the workspace is still loading', () => {
    vi.mocked(useProjectDetailQuery).mockReturnValue(idleQuery({ isLoading: true }) as never);
    vi.mocked(useProjectAnnotationWorkspaceQuery).mockReturnValue(idleQuery() as never);
    vi.mocked(useProjectParticipantAnnotationWorkspaceQuery).mockReturnValue(idleQuery() as never);

    const { result } = renderHook(() =>
      useAnnotationWorkspace({
        annotationOffset: 0,
        isInvalidProjectId: false,
        isReviewMode: false,
        numericProjectId: 1,
        reviewedParticipantUserId: null,
      }),
    );

    expect(result.current.canRenderWorkspace).toBe(false);
  });

  it('is ready to render once project and workspace both loaded without errors', () => {
    vi.mocked(useProjectDetailQuery).mockReturnValue(idleQuery({ data: project }) as never);
    vi.mocked(useProjectAnnotationWorkspaceQuery).mockReturnValue(idleQuery({ data: workspace }) as never);
    vi.mocked(useProjectParticipantAnnotationWorkspaceQuery).mockReturnValue(idleQuery() as never);

    const { result } = renderHook(() =>
      useAnnotationWorkspace({
        annotationOffset: 0,
        isInvalidProjectId: false,
        isReviewMode: false,
        numericProjectId: 1,
        reviewedParticipantUserId: null,
      }),
    );

    expect(result.current.canRenderWorkspace).toBe(true);
  });

  it('falls back to the project labels/type when the workspace has not loaded yet', () => {
    vi.mocked(useProjectDetailQuery).mockReturnValue(idleQuery({ data: project }) as never);
    vi.mocked(useProjectAnnotationWorkspaceQuery).mockReturnValue(idleQuery() as never);
    vi.mocked(useProjectParticipantAnnotationWorkspaceQuery).mockReturnValue(idleQuery() as never);

    const { result } = renderHook(() =>
      useAnnotationWorkspace({
        annotationOffset: 0,
        isInvalidProjectId: false,
        isReviewMode: false,
        numericProjectId: 1,
        reviewedParticipantUserId: null,
      }),
    );

    expect(result.current.annotationProjectType).toBe('TEXT_CLASSIFICATION_SIMPLE');
    expect(result.current.labels).toEqual(project.labels);
  });
});
