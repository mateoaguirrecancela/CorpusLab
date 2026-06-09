import type { AuthMessageResponse } from '@/modules/auth/types/common';

export type ForgotPasswordFormState = {
  email: string;
};

export type ForgotPasswordResponse = AuthMessageResponse;
