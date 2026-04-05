import { type FormEventHandler, useMemo, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router';
import { INITIAL_LOGIN_STATE } from '@/modules/auth/constants/login';
import { type OAuthProvider } from '@/modules/auth/constants/session';
import {
  getLoginErrorMessage,
  getLogoutErrorMessage,
  login,
  logout,
  redirectToOAuthAuthorization,
} from '@/modules/auth/services/authService';
import { primeProfileCache } from '@/modules/auth/services/profileCache';
import { clearSession, storeSessionToken } from '@/modules/auth/services/sessionService';
import { type LoginFormState } from '@/modules/auth/types/login';
import { isEmailValid } from '@/modules/auth/utils/validation';

export function useSignInForm() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [form, setForm] = useState<LoginFormState>(INITIAL_LOGIN_STATE);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  const canSubmit = useMemo(
    () => isEmailValid(form.email) && form.password.length >= 8 && !isSubmitting,
    [form, isSubmitting],
  );

  const updateField = <K extends keyof LoginFormState>(field: K, value: LoginFormState[K]) => {
    setForm((current) => ({ ...current, [field]: value }));
  };

  const handleSubmit: FormEventHandler<HTMLFormElement> = async (event) => {
    event.preventDefault();

    if (!canSubmit) {
      setErrorMessage(t('auth.login.required'));
      setSuccessMessage('');
      return;
    }

    setIsSubmitting(true);
    setErrorMessage('');
    setSuccessMessage('');

    try {
      const response = await login(form);
      storeSessionToken(response.token);
      await primeProfileCache(queryClient, {
        email: response.email,
        firstName: response.firstName,
        lastName: response.lastName,
      });

      setIsLoggedIn(true);
      setSuccessMessage(t('auth.login.welcome', { name: response.firstName }));
      setForm((current) => ({ ...current, password: '' }));
      navigate('/home');
    } catch (error) {
      setErrorMessage(getLoginErrorMessage(error));
      setIsLoggedIn(false);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleLogout = async () => {
    setIsSubmitting(true);
    setErrorMessage('');

    try {
      const response = await logout();
      clearSession(queryClient);
      setIsLoggedIn(false);
      setSuccessMessage(response.message);
    } catch (error) {
      setErrorMessage(getLogoutErrorMessage(error));
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
    isLoggedIn,
    errorMessage,
    successMessage,
    updateField,
    handleSubmit,
    handleLogout,
    handleOAuthClick,
  };
}
