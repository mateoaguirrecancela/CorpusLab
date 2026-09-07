import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { EditProjectDialog } from '@/modules/project/core/components/EditProjectDialog';
import { useEditProjectDialog } from '@/modules/project/core/hooks/useEditProjectDialog';
import { renderWithRouter } from '@/test/testUtils';

vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string, opts?: Record<string, unknown>) =>
      opts ? `${key}:${JSON.stringify(opts)}` : key,
  }),
}));

vi.mock('@/modules/project/core/hooks/useEditProjectDialog', () => ({
  useEditProjectDialog: vi.fn(),
}));

const handleUpdateProject = vi.fn();
const handleDeleteRequest = vi.fn();
const handleDeleteProject = vi.fn();
const handleOpenChange = vi.fn();
const setName = vi.fn();
const setDescription = vi.fn();
const toggleSelection = vi.fn();

function baseHookState(overrides: Partial<ReturnType<typeof useEditProjectDialog>> = {}) {
  return {
    assignmentByUserId: {},
    canSave: true,
    confirmDeleteOpen: false,
    creatorUserId: 9,
    description: 'Some description',
    errors: {},
    handleDeleteProject,
    handleDeleteRequest,
    handleOpenChange,
    handleUpdateProject,
    isBusy: false,
    isDeleting: false,
    isLoadingMembers: false,
    isUpdating: false,
    members: [{ userId: 9, firstName: 'Jane', lastName: 'Doe', email: 'a@b.com' }],
    name: 'My project',
    open: true,
    setConfirmDeleteOpen: vi.fn(),
    setDescription,
    setName,
    toggleSelection,
    ...overrides,
  } as ReturnType<typeof useEditProjectDialog>;
}

function baseProps(overrides: Partial<Parameters<typeof EditProjectDialog>[0]> = {}) {
  return {
    groupId: 1,
    projectId: 10,
    initialName: 'My project',
    initialDescription: 'Some description',
    initialParticipantAssignments: [],
    ...overrides,
  };
}

beforeEach(() => {
  vi.mocked(useEditProjectDialog).mockReturnValue(baseHookState());
});

describe('EditProjectDialog', () => {
  it('renders the current name and description', () => {
    renderWithRouter(<EditProjectDialog {...baseProps()} />);

    expect(screen.getByDisplayValue('My project')).toBeInTheDocument();
    expect(screen.getByDisplayValue('Some description')).toBeInTheDocument();
  });

  it('reports name and description edits to the hook', async () => {
    const user = userEvent.setup();
    renderWithRouter(<EditProjectDialog {...baseProps()} />);

    await user.type(screen.getByLabelText('project.edit.nameLabel *', { exact: false }), 'X');

    expect(setName).toHaveBeenCalled();
  });

  it('disables the save button when the hook reports it cannot save', () => {
    vi.mocked(useEditProjectDialog).mockReturnValue(baseHookState({ canSave: false }));
    renderWithRouter(<EditProjectDialog {...baseProps()} />);

    expect(screen.getByRole('button', { name: 'project.edit.submit' })).toBeDisabled();
  });

  it('calls handleUpdateProject when save is clicked', async () => {
    const user = userEvent.setup();
    renderWithRouter(<EditProjectDialog {...baseProps()} />);

    await user.click(screen.getByRole('button', { name: 'project.edit.submit' }));

    expect(handleUpdateProject).toHaveBeenCalled();
  });

  it('shows a saving indicator while the update is pending', () => {
    vi.mocked(useEditProjectDialog).mockReturnValue(baseHookState({ isUpdating: true }));
    renderWithRouter(<EditProjectDialog {...baseProps()} />);

    expect(screen.getByText('common.actions.saving')).toBeInTheDocument();
  });

  it('hides the delete button unless showDeleteButton is set', () => {
    renderWithRouter(<EditProjectDialog {...baseProps()} />);

    expect(screen.queryByRole('button', { name: 'project.edit.delete' })).not.toBeInTheDocument();
  });

  it('shows the delete button and wires it to handleDeleteRequest', async () => {
    const user = userEvent.setup();
    renderWithRouter(<EditProjectDialog {...baseProps({ showDeleteButton: true })} />);

    await user.click(screen.getByRole('button', { name: 'project.edit.delete' }));

    expect(handleDeleteRequest).toHaveBeenCalled();
  });

  it('surfaces a name validation error from the hook', () => {
    vi.mocked(useEditProjectDialog).mockReturnValue(
      baseHookState({ errors: { name: { type: 'too_small', message: 'Name is required' } } }),
    );
    renderWithRouter(<EditProjectDialog {...baseProps()} />);

    expect(screen.getByText('Name is required')).toBeInTheDocument();
  });

  it('opens the confirm-delete dialog and wires it to the hook handlers', async () => {
    const user = userEvent.setup();
    // Mirrors the real hook: handleDeleteRequest closes the edit dialog
    // before opening the confirm dialog, so the two are never both "open".
    vi.mocked(useEditProjectDialog).mockReturnValue(
      baseHookState({ open: false, confirmDeleteOpen: true }),
    );
    renderWithRouter(<EditProjectDialog {...baseProps()} />);

    expect(screen.getByText('project.edit.deleteTitle')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'project.edit.delete' }));

    expect(handleDeleteProject).toHaveBeenCalled();
  });

  it('shows a spinner and disables the confirm button while deleting', () => {
    vi.mocked(useEditProjectDialog).mockReturnValue(
      baseHookState({ open: false, confirmDeleteOpen: true, isDeleting: true }),
    );
    renderWithRouter(<EditProjectDialog {...baseProps()} />);

    expect(screen.getByRole('button', { name: 'project.edit.deleting' })).toBeDisabled();
  });

  it('shows the loading state for the assignable members list', () => {
    vi.mocked(useEditProjectDialog).mockReturnValue(baseHookState({ isLoadingMembers: true }));
    renderWithRouter(<EditProjectDialog {...baseProps()} />);

    expect(screen.getByText('project.create.loadingMembers')).toBeInTheDocument();
  });
});
