import { type FormEvent, useMemo, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router';
import { INITIAL_LOGIN_STATE } from '@/modules/auth/constants/login';
import {
  type OAuthProvider,
  SESSION_AUTH_TOKEN_STORAGE_KEY,
} from '@/modules/auth/constants/session';
import { PROFILE_QUERY_KEY } from '@/modules/auth/hooks/useProfileQuery';
import {
  getProfile,
  getLoginErrorMessage,
  getLogoutErrorMessage,
  login,
  logout,
  redirectToOAuthAuthorization,
} from '@/modules/auth/services/authService';
import { type LoginFormState } from '@/modules/auth/types/login';
import { type ProfileResponse } from '@/modules/auth/types/profile';

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

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
    () => EMAIL_REGEX.test(form.email.trim()) && form.password.length >= 8 && !isSubmitting,
    [form, isSubmitting],
  );

  const updateField = <K extends keyof LoginFormState>(field: K, value: LoginFormState[K]) => {
    setForm((current) => ({ ...current, [field]: value }));
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
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
      localStorage.setItem(SESSION_AUTH_TOKEN_STORAGE_KEY, response.token);

      try {
        await queryClient.fetchQuery({
          queryKey: PROFILE_QUERY_KEY,
          queryFn: getProfile,
        });
      } catch {
        queryClient.setQueryData<ProfileResponse>(PROFILE_QUERY_KEY, {
          email: response.email,
          firstName: response.firstName,
          lastName: response.lastName,
          birth: null,
          gender: null,
          countryCode: null,
          city: null,
        });
      }

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
      localStorage.removeItem(SESSION_AUTH_TOKEN_STORAGE_KEY);
      queryClient.removeQueries({ queryKey: PROFILE_QUERY_KEY });
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
