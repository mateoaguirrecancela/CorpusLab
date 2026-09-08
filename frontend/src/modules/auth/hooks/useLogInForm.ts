import { type FormEventHandler, useMemo } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router';
import { INITIAL_LOGIN_STATE } from '@/modules/auth/constants/login';
import { getLoginErrorMessage, login } from '@/modules/auth/services/authService';
import { storeAuthenticatedSession } from '@/modules/auth/services/sessionService';
import { type LoginFormState } from '@/modules/auth/types/login';
import { createLoginSchema, type LoginFormValues } from '@/modules/auth/schemas/authFormSchemas';
import { useAuthFormController } from '@/modules/auth/hooks/useAuthFormController';
import { useOAuthAuthorization } from '@/modules/auth/hooks/useOAuthAuthorization';

const LOGIN_FIELDS = ['email', 'password'] as const satisfies readonly (keyof LoginFormValues)[];

export function useLogInForm() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const loginSchema = useMemo(() => createLoginSchema(t), [t]);
  const {
    form,
    canSubmit,
    fieldErrors,
    isSubmitting,
    errorMessage,
    successMessage,
    setErrorMessage,
    setSuccessMessage,
    submitForm,
    updateField,
    methods: { resetField },
  } = useAuthFormController<LoginFormValues>({
    defaultValues: INITIAL_LOGIN_STATE,
    fieldNames: LOGIN_FIELDS,
    schema: loginSchema,
  });

  const loginValues: LoginFormState = form;
  const handleOAuthClick = useOAuthAuthorization(isSubmitting);

  const handleSubmit: FormEventHandler<HTMLFormElement> = async (event) => {
    await submitForm({
      event,
      invalidMessage: t('auth.login.required'),
      onError: (error) => setErrorMessage(getLoginErrorMessage(error)),
      onValid: async (values) => {
        const response = await login(values);
        storeAuthenticatedSession(queryClient, response);
        setSuccessMessage(t('auth.login.welcome', { name: response.firstName }));
        resetField('password');
        navigate('/home');
      },
    });
  };

  return {
    form: loginValues,
    canSubmit,
    fieldErrors,
    isSubmitting,
    errorMessage,
    successMessage,
    updateField,
    handleSubmit,
    handleOAuthClick,
  };
}
