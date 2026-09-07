import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { ConfirmDestructiveDialog } from '@/components/common/ConfirmDestructiveDialog';

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (key: string) => key }),
}));

describe('ConfirmDestructiveDialog', () => {
  it('renders nothing when closed', () => {
    render(
      <ConfirmDestructiveDialog
        confirmLabel="Delete"
        description="This cannot be undone"
        onConfirm={vi.fn()}
        onOpenChange={vi.fn()}
        open={false}
        title="Delete project"
      />,
    );

    expect(screen.queryByText('Delete project')).not.toBeInTheDocument();
  });

  it('shows the title and description when open', () => {
    render(
      <ConfirmDestructiveDialog
        confirmLabel="Delete"
        description="This cannot be undone"
        onConfirm={vi.fn()}
        onOpenChange={vi.fn()}
        open
        title="Delete project"
      />,
    );

    expect(screen.getByText('Delete project')).toBeInTheDocument();
    expect(screen.getByText('This cannot be undone')).toBeInTheDocument();
  });

  it('calls onOpenChange(false) when cancel is clicked', async () => {
    const user = userEvent.setup();
    const onOpenChange = vi.fn();
    render(
      <ConfirmDestructiveDialog
        confirmLabel="Delete"
        description="This cannot be undone"
        onConfirm={vi.fn()}
        onOpenChange={onOpenChange}
        open
        title="Delete project"
      />,
    );

    await user.click(screen.getByRole('button', { name: 'common.actions.cancel' }));

    expect(onOpenChange).toHaveBeenCalledWith(false);
  });

  it('calls onConfirm when the destructive action is clicked', async () => {
    const user = userEvent.setup();
    const onConfirm = vi.fn();
    render(
      <ConfirmDestructiveDialog
        confirmLabel="Delete"
        description="This cannot be undone"
        onConfirm={onConfirm}
        onOpenChange={vi.fn()}
        open
        title="Delete project"
      />,
    );

    await user.click(screen.getByRole('button', { name: 'Delete' }));

    expect(onConfirm).toHaveBeenCalled();
  });

  it('disables both buttons and shows the confirming label while confirming', () => {
    render(
      <ConfirmDestructiveDialog
        confirmLabel="Delete"
        confirmingLabel="Deleting..."
        description="This cannot be undone"
        isConfirming
        onConfirm={vi.fn()}
        onOpenChange={vi.fn()}
        open
        title="Delete project"
      />,
    );

    expect(screen.getByRole('button', { name: 'common.actions.cancel' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Deleting...' })).toBeDisabled();
  });

  it('hides the built-in close button while confirming', () => {
    const { rerender } = render(
      <ConfirmDestructiveDialog
        confirmLabel="Delete"
        description="This cannot be undone"
        onConfirm={vi.fn()}
        onOpenChange={vi.fn()}
        open
        title="Delete project"
      />,
    );
    expect(screen.getByRole('button', { name: 'Close' })).toBeInTheDocument();

    rerender(
      <ConfirmDestructiveDialog
        confirmLabel="Delete"
        description="This cannot be undone"
        isConfirming
        onConfirm={vi.fn()}
        onOpenChange={vi.fn()}
        open
        title="Delete project"
      />,
    );

    expect(screen.queryByRole('button', { name: 'Close' })).not.toBeInTheDocument();
  });
});
