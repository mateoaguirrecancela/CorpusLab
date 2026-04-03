import { type FormEvent, useMemo, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router';
import { INITIAL_REGISTER_STATE } from '@/modules/auth/constants/signup';
import {
  type OAuthProvider,
  SESSION_AUTH_TOKEN_STORAGE_KEY,
} from '@/modules/auth/constants/session';
import { PROFILE_QUERY_KEY } from '@/modules/auth/hooks/useProfileQuery';
import {
  getProfile,
  getLoginErrorMessage,
  getRegisterErrorMessage,
  login,
  redirectToOAuthAuthorization,
  signup,
} from '@/modules/auth/services/authService';
import { type RegisterFormState } from '@/modules/auth/types/signup';
import { type ProfileResponse } from '@/modules/auth/types/profile';

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

function isAtLeast16YearsOld(birthDate: string): boolean {
  if (!birthDate) {
    return false;
  }

  const [yearText, monthText, dayText] = birthDate.split('-');
  const year = Number(yearText);
  const month = Number(monthText);
  const day = Number(dayText);

  if (!Number.isInteger(year) || !Number.isInteger(month) || !Number.isInteger(day)) {
    return false;
  }

  const today = new Date();
  let age = today.getFullYear() - year;
  const currentMonth = today.getMonth() + 1;
  const currentDay = today.getDate();

  if (currentMonth < month || (currentMonth === month && currentDay < day)) {
    age -= 1;
  }

  return age >= 16;
}

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
      EMAIL_REGEX.test(form.email.trim()) &&
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

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
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

      localStorage.setItem(SESSION_AUTH_TOKEN_STORAGE_KEY, loginResponse.token);

      try {
        await queryClient.fetchQuery({
          queryKey: PROFILE_QUERY_KEY,
          queryFn: getProfile,
        });
      } catch {
        queryClient.setQueryData<ProfileResponse>(PROFILE_QUERY_KEY, {
          email: loginResponse.email,
          firstName: loginResponse.firstName,
          lastName: loginResponse.lastName,
          birth: null,
          gender: null,
          countryCode: null,
          city: null,
        });
      }

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
