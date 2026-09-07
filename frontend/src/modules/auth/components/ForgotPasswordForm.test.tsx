import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { ForgotPasswordForm } from '@/modules/auth/components/ForgotPasswordForm';
import { renderWithRouter } from '@/test/testUtils';

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (key: string) => key }),
}));

vi.mock('sonner', () => ({
  toast: { error: vi.fn(), success: vi.fn() },
}));

function baseProps(overrides: Partial<Parameters<typeof ForgotPasswordForm>[0]> = {}) {
  return {
    form: { email: '' },
    canSubmit: false,
    fieldErrors: {},
    isSubmitting: false,
    errorMessage: '',
    successMessage: '',
    onSubmit: vi.fn(),
    onFieldChange: vi.fn(),
    ...overrides,
  };
}

describe('ForgotPasswordForm', () => {
  it('reports keystrokes through onFieldChange', async () => {
    const user = userEvent.setup();
    const onFieldChange = vi.fn();
    renderWithRouter(<ForgotPasswordForm {...baseProps({ onFieldChange })} />);

    await user.type(screen.getByLabelText('auth.forgotPassword.email', { exact: false }), 'a');

    expect(onFieldChange).toHaveBeenCalledWith('email', 'a');
  });

  it('disables submit until the form can be submitted', () => {
    renderWithRouter(<ForgotPasswordForm {...baseProps({ canSubmit: false })} />);

    expect(screen.getByRole('button', { name: 'auth.forgotPassword.submit' })).toBeDisabled();
  });

  it('submits the form when clicked', async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn((event: React.FormEvent) => event.preventDefault());
    renderWithRouter(<ForgotPasswordForm {...baseProps({ canSubmit: true, onSubmit })} />);

    await user.click(screen.getByRole('button', { name: 'auth.forgotPassword.submit' }));

    expect(onSubmit).toHaveBeenCalled();
  });

  it('shows a field-level error', () => {
    renderWithRouter(
      <ForgotPasswordForm
        {...baseProps({ fieldErrors: { email: 'auth.login.invalidEmail' } })}
      />,
    );

    expect(screen.getByText('auth.login.invalidEmail')).toBeInTheDocument();
  });

  it('shows the submitting label while in flight', () => {
    renderWithRouter(
      <ForgotPasswordForm {...baseProps({ canSubmit: true, isSubmitting: true })} />,
    );

    expect(
      screen.getByRole('button', { name: 'auth.forgotPassword.submitting' }),
    ).toBeDisabled();
  });
});
