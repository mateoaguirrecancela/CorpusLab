import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { toast } from 'sonner';
import { InviteResearchGroupMemberDialog } from '@/modules/researchgroup/components/InviteResearchGroupMemberDialog';
import { useInviteResearchGroupMemberMutation } from '@/modules/researchgroup/hooks/useResearchGroupQueries';
import { getInviteResearcherErrorMessage } from '@/modules/researchgroup/services/researchGroupService';

vi.mock('react-i18next', () => {
  const t = (key: string) => key;
  return { useTranslation: () => ({ t }) };
});

vi.mock('sonner', () => ({
  toast: { error: vi.fn(), success: vi.fn() },
}));

vi.mock('@/modules/researchgroup/hooks/useResearchGroupQueries', () => ({
  useInviteResearchGroupMemberMutation: vi.fn(),
}));

vi.mock('@/modules/researchgroup/services/researchGroupService', () => ({
  getInviteResearcherErrorMessage: vi.fn(() => 'invite-error'),
}));

const mutateAsync = vi.fn();
let writeText: ReturnType<typeof vi.spyOn>;

async function openDialog() {
  // @testing-library/user-event stubs navigator.clipboard as part of its own
  // setup(), so it only exists to spy on after this call runs.
  const user = userEvent.setup();
  writeText = vi.spyOn(navigator.clipboard, 'writeText').mockResolvedValue(undefined);
  render(
    <InviteResearchGroupMemberDialog
      groupId={1}
      invitationCode="ABC123"
      trigger={<button type="button">Open</button>}
    />,
  );
  await user.click(screen.getByRole('button', { name: 'Open' }));
  return user;
}

beforeEach(() => {
  mutateAsync.mockReset().mockResolvedValue(undefined);
  vi.mocked(toast.error).mockClear();
  vi.mocked(toast.success).mockClear();
  vi.mocked(useInviteResearchGroupMemberMutation).mockReturnValue({
    mutateAsync,
    isPending: false,
  } as never);
});

afterEach(() => {
  writeText?.mockRestore();
});

describe('InviteResearchGroupMemberDialog', () => {
  it('opens on trigger click and shows the invitation code', async () => {
    await openDialog();

    expect(screen.getByText('researchGroup.invite.title')).toBeInTheDocument();
    expect(screen.getByText('ABC123')).toBeInTheDocument();
  });

  it('cannot submit until a valid email and role are set', async () => {
    await openDialog();

    expect(screen.getByRole('button', { name: 'researchGroup.invite.submit' })).toBeDisabled();
  });

  it('enables submit once a valid email is entered (role has a default)', async () => {
    const user = await openDialog();

    await user.type(
      screen.getByLabelText('researchGroup.invite.emailLabel', { exact: false }),
      'a@b.com',
    );

    await waitFor(() =>
      expect(screen.getByRole('button', { name: 'researchGroup.invite.submit' })).toBeEnabled(),
    );
  });

  it('copies the invitation code and shows a success toast', async () => {
    const user = await openDialog();

    await user.click(screen.getByRole('button', { name: 'researchGroup.detail.copyCode' }));

    expect(writeText).toHaveBeenCalledWith('ABC123');
    expect(toast.success).toHaveBeenCalledWith('researchGroup.detail.codeCopied');
  });

  it('shows an error toast when copying the invitation code fails', async () => {
    const user = await openDialog();
    writeText.mockRejectedValue(new Error('denied'));

    await user.click(screen.getByRole('button', { name: 'researchGroup.detail.copyCode' }));

    await waitFor(() =>
      expect(toast.error).toHaveBeenCalledWith('researchGroup.detail.codeCopyFailed'),
    );
  });

  it('invites the member and shows a success toast on submit', async () => {
    const user = await openDialog();
    await user.type(
      screen.getByLabelText('researchGroup.invite.emailLabel', { exact: false }),
      'a@b.com',
    );
    await waitFor(() =>
      expect(screen.getByRole('button', { name: 'researchGroup.invite.submit' })).toBeEnabled(),
    );

    await user.click(screen.getByRole('button', { name: 'researchGroup.invite.submit' }));

    await waitFor(() => expect(mutateAsync).toHaveBeenCalledWith({ email: 'a@b.com', role: 'ANNOTATOR' }));
    expect(toast.success).toHaveBeenCalledWith('researchGroup.invite.success');
  });

  it('shows the mapped error toast when the invite fails', async () => {
    mutateAsync.mockRejectedValue(new Error('boom'));
    const user = await openDialog();
    await user.type(
      screen.getByLabelText('researchGroup.invite.emailLabel', { exact: false }),
      'a@b.com',
    );
    await waitFor(() =>
      expect(screen.getByRole('button', { name: 'researchGroup.invite.submit' })).toBeEnabled(),
    );

    await user.click(screen.getByRole('button', { name: 'researchGroup.invite.submit' }));

    await waitFor(() => expect(getInviteResearcherErrorMessage).toHaveBeenCalled());
    expect(toast.error).toHaveBeenCalledWith('invite-error');
  });

  it('resets the email field after closing and reopening the dialog', async () => {
    const user = await openDialog();
    await user.type(
      screen.getByLabelText('researchGroup.invite.emailLabel', { exact: false }),
      'a@b.com',
    );
    await waitFor(() =>
      expect(
        screen.getByLabelText('researchGroup.invite.emailLabel', { exact: false }),
      ).toHaveValue('a@b.com'),
    );

    await user.click(screen.getByRole('button', { name: 'Close' }));
    await user.click(screen.getByRole('button', { name: 'Open' }));

    expect(
      screen.getByLabelText('researchGroup.invite.emailLabel', { exact: false }),
    ).toHaveValue('');
  });

  it('shows a saving indicator and disables submit while the mutation is pending', async () => {
    vi.mocked(useInviteResearchGroupMemberMutation).mockReturnValue({
      mutateAsync,
      isPending: true,
    } as never);
    await openDialog();

    expect(screen.getByRole('button', { name: 'common.actions.saving' })).toBeDisabled();
  });
});
