import type { AuthMessageResponse } from '@/modules/auth/types/common';

export type ResetPasswordFormState = {
  token: string;
  newPassword: string;
  confirmPassword: string;
};

export type ResetPasswordPayload = {
  token: string;
  newPassword: string;
};

export type ResetPasswordResponse = AuthMessageResponse;
