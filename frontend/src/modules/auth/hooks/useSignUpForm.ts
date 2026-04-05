import { type FormEventHandler, useMemo, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router';
import { INITIAL_REGISTER_STATE } from '@/modules/auth/constants/signup';
import { type OAuthProvider } from '@/modules/auth/constants/session';
import {
  getLoginErrorMessage,
  getRegisterErrorMessage,
  login,
  redirectToOAuthAuthorization,
  signup,
} from '@/modules/auth/services/authService';
import { primeProfileCache } from '@/modules/auth/services/profileCache';
import { storeSessionToken } from '@/modules/auth/services/sessionService';
import { type RegisterFormState } from '@/modules/auth/types/signup';
import { isAtLeast16YearsOld, isEmailValid } from '@/modules/auth/utils/validation';

export function useSignUpForm() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [form, setForm] = useState<RegisterFormState>(INITIAL_REGISTER_STATE);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  const canSubmit = useMemo(
    () =>
      form.firstName.trim().length > 0 &&
      form.lastName.trim().length > 0 &&
      isEmailValid(form.email) &&
      form.password.length >= 8 &&
      isAtLeast16YearsOld(form.birth) &&
      form.gender.trim().length > 0 &&
      form.countryCode.length === 2 &&
      form.city.trim().length > 0 &&
      !isSubmitting,
    [form, isSubmitting],
  );

  const updateField = <K extends keyof RegisterFormState>(
    field: K,
    value: RegisterFormState[K],
  ) => {
    setForm((current) => ({ ...current, [field]: value }));
  };

  const handleSubmit: FormEventHandler<HTMLFormElement> = async (event) => {
    event.preventDefault();

    if (form.birth.length > 0 && !isAtLeast16YearsOld(form.birth)) {
      setErrorMessage(t('auth.signup.underAge'));
      setSuccessMessage('');
      return;
    }

    if (!canSubmit) {
      setErrorMessage(t('auth.signup.required'));
      setSuccessMessage('');
      return;
    }

    setIsSubmitting(true);
    setErrorMessage('');
    setSuccessMessage('');

    try {
      await signup(form);
      const loginResponse = await login({
        email: form.email,
        password: form.password,
      });

      storeSessionToken(loginResponse.token);
      await primeProfileCache(queryClient, {
        email: loginResponse.email,
        firstName: loginResponse.firstName,
        lastName: loginResponse.lastName,
      });

      setSuccessMessage(t('auth.signup.success', { name: loginResponse.firstName }));
      setForm(INITIAL_REGISTER_STATE);
      navigate('/home');
    } catch (error) {
      const registerErrorMessage = getRegisterErrorMessage(error);
      const loginErrorMessage = getLoginErrorMessage(error);
      const isRegisterError = registerErrorMessage !== t('auth.errors.unexpected.signup');
      setErrorMessage(isRegisterError ? registerErrorMessage : loginErrorMessage);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleOAuthClick = (provider: OAuthProvider) => {
    if (isSubmitting) {
      return;
    }

    redirectToOAuthAuthorization(provider);
  };

  return {
    form,
    canSubmit,
    isSubmitting,
    errorMessage,
    successMessage,
    updateField,
    handleSubmit,
    handleOAuthClick,
  };
}
