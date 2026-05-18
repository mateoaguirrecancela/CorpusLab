import { type FormEventHandler, useMemo } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router';
import { INITIAL_REGISTER_STATE } from '@/modules/auth/constants/signup';
import { getRegisterErrorMessage, signup } from '@/modules/auth/services/authService';
import { storeAuthenticatedSession } from '@/modules/auth/services/sessionService';
import { type RegisterFormState } from '@/modules/auth/types/signup';
import { createSignupSchema, type SignupFormValues } from '@/modules/auth/schemas/authFormSchemas';
import { useAuthFormController } from '@/modules/auth/hooks/useAuthFormController';
import { useOAuthAuthorization } from '@/modules/auth/hooks/useOAuthAuthorization';

const SIGNUP_FIELDS = [
  'birth',
  'city',
  'countryCode',
  'email',
  'firstName',
  'gender',
  'lastName',
  'password',
] as const satisfies readonly (keyof SignupFormValues)[];

export function useSignUpForm() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const signupSchema = useMemo(() => createSignupSchema(t), [t]);
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
    methods: { reset },
  } = useAuthFormController<SignupFormValues>({
    defaultValues: INITIAL_REGISTER_STATE,
    fieldNames: SIGNUP_FIELDS,
    schema: signupSchema,
  });

  const registerValues: RegisterFormState = form;
  const handleOAuthClick = useOAuthAuthorization(isSubmitting);

  const handleSubmit: FormEventHandler<HTMLFormElement> = async (event) => {
    await submitForm({
      event,
      invalidMessage: t('auth.signup.required'),
      onError: (error) => setErrorMessage(getRegisterErrorMessage(error)),
      onValid: async (values) => {
        const response = await signup(values);
        storeAuthenticatedSession(queryClient, response);
        setSuccessMessage(t('auth.signup.success', { name: response.firstName }));
        reset(INITIAL_REGISTER_STATE);
        navigate('/home');
      },
    });
  };

  return {
    form: registerValues,
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
