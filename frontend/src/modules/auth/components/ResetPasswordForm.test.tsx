import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { ResetPasswordForm } from '@/modules/auth/components/ResetPasswordForm';
import { renderWithRouter } from '@/test/testUtils';

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (key: string) => key }),
}));

vi.mock('sonner', () => ({
  toast: { error: vi.fn(), success: vi.fn() },
}));

function baseProps(overrides: Partial<Parameters<typeof ResetPasswordForm>[0]> = {}) {
  return {
    form: { token: 'tok', newPassword: '', confirmPassword: '' },
    canSubmit: false,
    fieldErrors: {},
    isSubmitting: false,
    errorMessage: '',
    onSubmit: vi.fn(),
    onFieldChange: vi.fn(),
    ...overrides,
  };
}

describe('ResetPasswordForm', () => {
  it('reports password keystrokes through onFieldChange', async () => {
    const user = userEvent.setup();
    const onFieldChange = vi.fn();
    renderWithRouter(<ResetPasswordForm {...baseProps({ onFieldChange })} />);

    await user.type(
      screen.getByLabelText('auth.resetPassword.newPassword', { exact: false }),
      'a',
    );

    expect(onFieldChange).toHaveBeenCalledWith('newPassword', 'a');
  });

  it('shows a mismatch error under the confirm password field', () => {
    renderWithRouter(
      <ResetPasswordForm
        {...baseProps({ fieldErrors: { confirmPassword: 'auth.resetPassword.passwordMismatch' } })}
      />,
    );

    expect(screen.getByText('auth.resetPassword.passwordMismatch')).toBeInTheDocument();
  });

  it('disables submit until the form can be submitted', () => {
    renderWithRouter(<ResetPasswordForm {...baseProps({ canSubmit: false })} />);

    expect(screen.getByRole('button', { name: 'auth.resetPassword.submit' })).toBeDisabled();
  });

  it('submits the form when clicked', async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn((event: React.FormEvent) => event.preventDefault());
    renderWithRouter(<ResetPasswordForm {...baseProps({ canSubmit: true, onSubmit })} />);

    await user.click(screen.getByRole('button', { name: 'auth.resetPassword.submit' }));

    expect(onSubmit).toHaveBeenCalled();
  });

  it('toggles password visibility independently for each field', async () => {
    const user = userEvent.setup();
    renderWithRouter(
      <ResetPasswordForm
        {...baseProps({ form: { token: 'tok', newPassword: 'secret1', confirmPassword: 'secret1' } })}
      />,
    );

    const newPasswordInput = screen.getByLabelText('auth.resetPassword.newPassword', {
      exact: false,
    });
    expect(newPasswordInput).toHaveAttribute('type', 'password');

    await user.click(screen.getAllByRole('button', { name: 'common.aria.showPassword' })[0]);

    expect(newPasswordInput).toHaveAttribute('type', 'text');
    expect(
      screen.getByLabelText('auth.resetPassword.confirmPassword', { exact: false }),
    ).toHaveAttribute('type', 'password');
  });
});
