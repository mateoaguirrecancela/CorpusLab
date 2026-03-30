export type ResetPasswordFormState = {
  token: string;
  newPassword: string;
  confirmPassword: string;
};

export type ResetPasswordPayload = {
  token: string;
  newPassword: string;
};

export type ResetPasswordResponse = {
  message: string;
};
