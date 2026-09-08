import { act, renderHook, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { toast } from 'sonner';
import { useResearchGroupInvitationsDialog } from '@/modules/researchgroup/hooks/useResearchGroupInvitationsDialog';
import {
  useAcceptResearchGroupInvitationMutation,
  useDeclineResearchGroupInvitationMutation,
  useJoinResearchGroupByCodeMutation,
  useResearchGroupInvitationsQuery,
} from '@/modules/researchgroup/hooks/useResearchGroupQueries';
import {
  getDeclineInvitationErrorMessage,
  getInvitationsErrorMessage,
  getJoinByCodeErrorMessage,
} from '@/modules/researchgroup/services/researchGroupService';
import { type ResearchGroupInvitation } from '@/modules/researchgroup/types/researchGroup';

vi.mock('react-i18next', () => {
  const t = (key: string) => key;
  return { useTranslation: () => ({ t }) };
});

vi.mock('sonner', () => ({
  toast: { error: vi.fn(), success: vi.fn() },
}));

vi.mock('@/modules/researchgroup/hooks/useResearchGroupQueries', () => ({
  useAcceptResearchGroupInvitationMutation: vi.fn(),
  useDeclineResearchGroupInvitationMutation: vi.fn(),
  useJoinResearchGroupByCodeMutation: vi.fn(),
  useResearchGroupInvitationsQuery: vi.fn(),
}));

vi.mock('@/modules/researchgroup/services/researchGroupService', () => ({
  getAcceptInvitationErrorMessage: vi.fn(() => 'accept-error'),
  getDeclineInvitationErrorMessage: vi.fn(() => 'decline-error'),
  getInvitationsErrorMessage: vi.fn(() => 'invitations-error'),
  getJoinByCodeErrorMessage: vi.fn(() => 'join-error'),
}));

const joinMutateAsync = vi.fn();
const acceptMutateAsync = vi.fn();
const declineMutateAsync = vi.fn();

const invitation: ResearchGroupInvitation = {
  id: 1,
  researchGroupId: 1,
  researchGroupName: 'Group',
  invitedEmail: 'a@b.com',
  inviterFullName: 'Jane Doe',
  role: 'ANNOTATOR',
  status: 'PENDING',
  createdAt: '2026-01-01T00:00:00Z',
  expiresAt: '2026-02-01T00:00:00Z',
};

beforeEach(() => {
  joinMutateAsync.mockReset().mockResolvedValue(undefined);
  acceptMutateAsync.mockReset().mockResolvedValue(undefined);
  declineMutateAsync.mockReset().mockResolvedValue(undefined);
  vi.mocked(toast.error).mockClear();
  vi.mocked(toast.success).mockClear();
  vi.mocked(useResearchGroupInvitationsQuery).mockReturnValue({
    data: [invitation],
    isLoading: false,
    isError: false,
    error: null,
  } as never);
  vi.mocked(useJoinResearchGroupByCodeMutation).mockReturnValue({
    mutateAsync: joinMutateAsync,
    isPending: false,
  } as never);
  vi.mocked(useAcceptResearchGroupInvitationMutation).mockReturnValue({
    mutateAsync: acceptMutateAsync,
    isPending: false,
    variables: undefined,
  } as never);
  vi.mocked(useDeclineResearchGroupInvitationMutation).mockReturnValue({
    mutateAsync: declineMutateAsync,
    isPending: false,
    variables: undefined,
  } as never);
});

describe('useResearchGroupInvitationsDialog', () => {
  it('reports the invitation list status', () => {
    const { result } = renderHook(() => useResearchGroupInvitationsDialog());

    expect(result.current.listStatus).toBe('ready');
    expect(result.current.invitations).toEqual([invitation]);
  });

  it('maps an invitations query error and shows a load-error toast', () => {
    vi.mocked(useResearchGroupInvitationsQuery).mockReturnValue({
      data: undefined,
      isLoading: false,
      isError: true,
      error: new Error('boom'),
    } as never);

    const { result } = renderHook(() => useResearchGroupInvitationsDialog());

    expect(getInvitationsErrorMessage).toHaveBeenCalled();
    expect(result.current.listStatus).toBe('error');
    expect(toast.error).toHaveBeenCalledWith('invitations-error', {
      id: 'research-group-invitations-load-error',
    });
  });

  it('cannot join by code until a valid code is entered', async () => {
    const { result } = renderHook(() => useResearchGroupInvitationsDialog());

    expect(result.current.canJoinByCode).toBe(false);

    act(() => result.current.setInvitationCode('ABC123'));

    await waitFor(() => expect(result.current.canJoinByCode).toBe(true));
  });

  it('joins by code, shows a success toast and resets the form', async () => {
    const { result } = renderHook(() => useResearchGroupInvitationsDialog());
    act(() => result.current.setInvitationCode('ABC123'));
    await waitFor(() => expect(result.current.canJoinByCode).toBe(true));

    await act(async () => {
      result.current.submitJoinByCode();
    });
    await waitFor(() => expect(joinMutateAsync).toHaveBeenCalledWith('ABC123'));

    await waitFor(() => expect(toast.success).toHaveBeenCalledWith('researchGroup.invitationsDialog.joinSuccess'));
    await waitFor(() => expect(result.current.invitationCode).toBe(''));
  });

  it('shows an error toast when joining by code fails', async () => {
    joinMutateAsync.mockRejectedValue(new Error('boom'));
    const { result } = renderHook(() => useResearchGroupInvitationsDialog());
    act(() => result.current.setInvitationCode('ABC123'));
    await waitFor(() => expect(result.current.canJoinByCode).toBe(true));

    await act(async () => {
      result.current.submitJoinByCode();
    });

    await waitFor(() => expect(getJoinByCodeErrorMessage).toHaveBeenCalled());
    expect(toast.error).toHaveBeenCalledWith('join-error');
  });

  it('accepts an invitation and shows a success toast', async () => {
    const { result } = renderHook(() => useResearchGroupInvitationsDialog());

    await act(async () => {
      await result.current.acceptInvitation(1);
    });

    expect(acceptMutateAsync).toHaveBeenCalledWith(1);
    expect(toast.success).toHaveBeenCalledWith('researchGroup.invitationsDialog.acceptSuccess');
  });

  it('shows an error toast when declining fails', async () => {
    declineMutateAsync.mockRejectedValue(new Error('boom'));
    const { result } = renderHook(() => useResearchGroupInvitationsDialog());

    await act(async () => {
      await result.current.declineInvitation(1);
    });

    expect(getDeclineInvitationErrorMessage).toHaveBeenCalled();
    expect(toast.error).toHaveBeenCalledWith('decline-error');
  });

  it('ignores an invitation action while another one is already pending', async () => {
    vi.mocked(useAcceptResearchGroupInvitationMutation).mockReturnValue({
      mutateAsync: acceptMutateAsync,
      isPending: true,
      variables: 1,
    } as never);
    const { result } = renderHook(() => useResearchGroupInvitationsDialog());

    await act(async () => {
      await result.current.declineInvitation(2);
    });

    expect(declineMutateAsync).not.toHaveBeenCalled();
  });

  it('closing the dialog resets the join-by-code form', async () => {
    const { result } = renderHook(() => useResearchGroupInvitationsDialog());
    act(() => result.current.setInvitationCode('ABC123'));
    await waitFor(() => expect(result.current.invitationCode).toBe('ABC123'));

    act(() => result.current.onOpenChange(false));

    await waitFor(() => expect(result.current.invitationCode).toBe(''));
  });
});
