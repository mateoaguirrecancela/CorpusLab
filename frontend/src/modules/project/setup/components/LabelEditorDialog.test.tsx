import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { LabelEditorDialog } from '@/modules/project/setup/components/LabelEditorDialog';
import { LABEL_COLOR_PALETTE } from '@/modules/project/setup/constants/labelColorPalette';

function baseProps(overrides: Partial<Parameters<typeof LabelEditorDialog>[0]> = {}) {
  return {
    colorLabel: 'Color',
    currentColor: '',
    currentName: '',
    inputLabel: 'Name',
    isNerProjectType: false,
    isOpen: true,
    mode: 'create' as const,
    onColorChange: vi.fn(),
    onNameChange: vi.fn(),
    onOpenChange: vi.fn(),
    onSave: vi.fn(),
    placeholder: 'e.g. Positive',
    saveCreateText: 'Add label',
    saveEditText: 'Save changes',
    titleCreate: 'New label',
    titleEdit: 'Edit label',
    ...overrides,
  };
}

describe('LabelEditorDialog', () => {
  it('shows the create title and text input in create mode', () => {
    render(<LabelEditorDialog {...baseProps()} />);

    expect(screen.getByText('New label')).toBeInTheDocument();
    expect(screen.getByLabelText('Name', { exact: false })).toBeInTheDocument();
  });

  it('shows the edit title and save-edit label in edit mode', () => {
    render(<LabelEditorDialog {...baseProps({ mode: 'edit', currentName: 'Positive' })} />);

    expect(screen.getByText('Edit label')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Save changes' })).toBeInTheDocument();
  });

  it('reports typed name changes', async () => {
    const user = userEvent.setup();
    const onNameChange = vi.fn();
    render(<LabelEditorDialog {...baseProps({ onNameChange })} />);

    await user.type(screen.getByLabelText('Name', { exact: false }), 'P');

    expect(onNameChange).toHaveBeenCalledWith('P');
  });

  it('renders a select with a disabled placeholder when csv label options are provided', () => {
    render(
      <LabelEditorDialog
        {...baseProps({
          labelNameOptions: [{ label: 'text', value: 'text' }],
        })}
      />,
    );

    const select = screen.getByLabelText('Name', { exact: false });
    expect(select.tagName).toBe('SELECT');
    expect(screen.getByRole('option', { name: 'e.g. Positive' })).toBeDisabled();
  });

  it('hides the color palette for non-NER project types', () => {
    render(<LabelEditorDialog {...baseProps({ isNerProjectType: false })} />);

    expect(screen.queryByLabelText(/Select color/)).not.toBeInTheDocument();
  });

  it('shows the color palette and reports the picked color for NER projects', async () => {
    const user = userEvent.setup();
    const onColorChange = vi.fn();
    render(<LabelEditorDialog {...baseProps({ isNerProjectType: true, onColorChange })} />);

    const swatches = screen.getAllByLabelText(/Select color/);
    expect(swatches).toHaveLength(LABEL_COLOR_PALETTE.length);

    await user.click(swatches[1]);

    expect(onColorChange).toHaveBeenCalledWith(LABEL_COLOR_PALETTE[1]);
  });

  it('disables the save button when isSaveDisabled is true', () => {
    render(<LabelEditorDialog {...baseProps({ isSaveDisabled: true })} />);

    expect(screen.getByRole('button', { name: 'Add label' })).toBeDisabled();
  });

  it('calls onSave when the save button is clicked', async () => {
    const user = userEvent.setup();
    const onSave = vi.fn();
    render(<LabelEditorDialog {...baseProps({ onSave })} />);

    await user.click(screen.getByRole('button', { name: 'Add label' }));

    expect(onSave).toHaveBeenCalled();
  });

  it('renders nothing when closed', () => {
    render(<LabelEditorDialog {...baseProps({ isOpen: false })} />);

    expect(screen.queryByText('New label')).not.toBeInTheDocument();
  });
});
