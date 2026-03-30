import { type ResetPasswordFormState } from '@/modules/auth/types/resetPassword';

export const buildInitialResetPasswordState = (token = ''): ResetPasswordFormState => ({
  token,
  newPassword: '',
  confirmPassword: '',
});
