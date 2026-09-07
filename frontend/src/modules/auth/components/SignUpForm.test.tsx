import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { SignUpForm } from '@/modules/auth/components/SignUpForm';
import { renderWithRouter } from '@/test/testUtils';

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ i18n: {}, t: (key: string) => key }),
}));

vi.mock('@/app/config/i18n', () => ({
  getResolvedLanguage: () => 'en-US',
}));

vi.mock('sonner', () => ({
  toast: { error: vi.fn(), success: vi.fn() },
}));

function baseProps(overrides: Partial<Parameters<typeof SignUpForm>[0]> = {}) {
  return {
    form: {
      firstName: '',
      lastName: '',
      email: '',
      password: '',
      birth: '',
      gender: '' as const,
      countryCode: '',
      city: '',
    },
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

describe('SignUpForm', () => {
  it('reports text field edits through onFieldChange', async () => {
    const user = userEvent.setup();
    const onFieldChange = vi.fn();
    renderWithRouter(<SignUpForm {...baseProps({ onFieldChange })} />);

    await user.type(screen.getByLabelText('auth.signup.firstName', { exact: false }), 'J');

    expect(onFieldChange).toHaveBeenCalledWith('firstName', 'J');
  });

  it('reports the selected gender through onFieldChange', async () => {
    const user = userEvent.setup();
    const onFieldChange = vi.fn();
    renderWithRouter(<SignUpForm {...baseProps({ onFieldChange })} />);

    await user.selectOptions(
      screen.getByLabelText('auth.signup.gender', { exact: false }),
      'FEMALE',
    );

    expect(onFieldChange).toHaveBeenCalledWith('gender', 'FEMALE');
  });

  it('disables the submit button while it cannot submit', () => {
    renderWithRouter(<SignUpForm {...baseProps({ canSubmit: false })} />);

    expect(screen.getByRole('button', { name: 'auth.signup.submit' })).toBeDisabled();
  });

  it('enables the submit button once the form reports it can submit', () => {
    renderWithRouter(<SignUpForm {...baseProps({ canSubmit: true })} />);

    expect(screen.getByRole('button', { name: 'auth.signup.submit' })).toBeEnabled();
  });

  it('shows field-level errors for every field', () => {
    renderWithRouter(
      <SignUpForm
        {...baseProps({
          fieldErrors: {
            city: 'auth.signup.required',
            email: 'auth.signup.invalidEmail',
          },
        })}
      />,
    );

    expect(screen.getByText('auth.signup.invalidEmail')).toBeInTheDocument();
    expect(screen.getByText('auth.signup.required')).toBeInTheDocument();
  });

  it('submits the form when the submit button is clicked', async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn((event: React.FormEvent) => event.preventDefault());
    renderWithRouter(<SignUpForm {...baseProps({ canSubmit: true, onSubmit })} />);

    await user.click(screen.getByRole('button', { name: 'auth.signup.submit' }));

    expect(onSubmit).toHaveBeenCalled();
  });

  it('renders the current form values back into the fields', () => {
    renderWithRouter(
      <SignUpForm
        {...baseProps({
          form: {
            firstName: 'Jane',
            lastName: 'Doe',
            email: 'a@b.com',
            password: 'password1',
            birth: '2000-01-01',
            gender: 'FEMALE',
            countryCode: '',
            city: 'Coruna',
          },
        })}
      />,
    );

    expect(screen.getByDisplayValue('Jane')).toBeInTheDocument();
    expect(screen.getByDisplayValue('Doe')).toBeInTheDocument();
    expect(screen.getByDisplayValue('Coruna')).toBeInTheDocument();
    expect(
      within(screen.getByLabelText('auth.signup.gender', { exact: false })).getByRole('option', {
        name: 'auth.gender.female',
        selected: true,
      }),
    ).toBeInTheDocument();
  });

  it('disables the OAuth buttons while submitting', () => {
    renderWithRouter(<SignUpForm {...baseProps({ isSubmitting: true })} />);

    expect(screen.getByRole('button', { name: /Google/ })).toBeDisabled();
  });
});
