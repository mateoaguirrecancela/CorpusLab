import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { LogInForm } from '@/modules/auth/components/LogInForm';
import { renderWithRouter } from '@/test/testUtils';

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (key: string) => key }),
}));

vi.mock('sonner', () => ({
  toast: { error: vi.fn(), success: vi.fn() },
}));

function baseProps(overrides: Partial<Parameters<typeof LogInForm>[0]> = {}) {
  return {
    form: { email: '', password: '' },
    canSubmit: false,
    fieldErrors: {},
    isSubmitting: false,
    errorMessage: '',
    successMessage: '',
    onSubmit: vi.fn(),
    onFieldChange: vi.fn(),
    onOAuthClick: vi.fn(),
    ...overrides,
  };
}

describe('LogInForm', () => {
  it('reports every keystroke through onFieldChange', async () => {
    const user = userEvent.setup();
    const onFieldChange = vi.fn();
    renderWithRouter(<LogInForm {...baseProps({ onFieldChange })} />);

    await user.type(screen.getByLabelText('auth.login.email', { exact: false }), 'a');

    expect(onFieldChange).toHaveBeenCalledWith('email', 'a');
  });

  it('submits the form when the submit button is clicked', async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn((event: React.FormEvent) => event.preventDefault());
    renderWithRouter(
      <LogInForm {...baseProps({ canSubmit: true, onSubmit })} />,
    );

    await user.click(screen.getByRole('button', { name: 'auth.login.submit' }));

    expect(onSubmit).toHaveBeenCalled();
  });

  it('disables the submit button while it cannot submit', () => {
    renderWithRouter(<LogInForm {...baseProps({ canSubmit: false })} />);

    expect(screen.getByRole('button', { name: 'auth.login.submit' })).toBeDisabled();
  });

  it('shows the submitting label and disables submit while in flight', () => {
    renderWithRouter(
      <LogInForm {...baseProps({ canSubmit: true, isSubmitting: true })} />,
    );

    expect(screen.getByRole('button', { name: 'auth.login.submitting' })).toBeDisabled();
  });

  it('shows field-level errors', () => {
    renderWithRouter(
      <LogInForm {...baseProps({ fieldErrors: { email: 'auth.login.invalidEmail' } })} />,
    );

    expect(screen.getByText('auth.login.invalidEmail')).toBeInTheDocument();
  });

  it('disables the OAuth buttons while submitting', () => {
    renderWithRouter(<LogInForm {...baseProps({ isSubmitting: true })} />);

    expect(screen.getByRole('button', { name: /Google/ })).toBeDisabled();
    expect(screen.getByRole('button', { name: /GitHub/ })).toBeDisabled();
  });

  it('forwards the clicked provider to onOAuthClick', async () => {
    const user = userEvent.setup();
    const onOAuthClick = vi.fn();
    renderWithRouter(<LogInForm {...baseProps({ onOAuthClick })} />);

    await user.click(screen.getByRole('button', { name: /Google/ }));

    expect(onOAuthClick).toHaveBeenCalledWith('google');
  });
});
