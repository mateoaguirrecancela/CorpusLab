import { type ForgotPasswordFormState } from '@/modules/auth/types/forgotPassword';
import { type LoginFormState } from '@/modules/auth/types/login';
import { type ResetPasswordPayload } from '@/modules/auth/types/resetPassword';
import { type UpdateProfilePayload } from '@/modules/auth/types/profile';
import { type RegisterFormState } from '@/modules/auth/types/signup';

type SignupPayload = {
  email: string;
  firstName: string;
  lastName: string;
  birth: string;
  gender: RegisterFormState['gender'] | undefined;
  countryCode: string;
  city: string;
  password: string;
};

type LoginPayload = {
  email: string;
  password: string;
};

type ForgotPasswordPayload = {
  email: string;
};

type ResetPasswordRequestPayload = {
  token: string;
  newPassword: string;
};

type OAuthExchangePayload = {
  code: string;
};

export function toSignupPayload(form: RegisterFormState): SignupPayload {
  return {
    email: form.email.trim(),
    firstName: form.firstName.trim(),
    lastName: form.lastName.trim(),
    birth: form.birth,
    gender: form.gender.length > 0 ? form.gender : undefined,
    countryCode: form.countryCode,
    city: form.city.trim(),
    password: form.password,
  };
}

export function toLoginPayload(form: LoginFormState): LoginPayload {
  return {
    email: form.email.trim(),
    password: form.password,
  };
}

export function toProfileUpdatePayload(payload: UpdateProfilePayload): UpdateProfilePayload {
  return {
    firstName: payload.firstName.trim(),
    lastName: payload.lastName.trim(),
    birth: payload.birth,
    gender: payload.gender,
    countryCode: payload.countryCode,
    city: payload.city,
  };
}

export function toForgotPasswordPayload(form: ForgotPasswordFormState): ForgotPasswordPayload {
  return {
    email: form.email.trim(),
  };
}

export function toResetPasswordPayload(payload: ResetPasswordPayload): ResetPasswordRequestPayload {
  return {
    token: payload.token.trim(),
    newPassword: payload.newPassword,
  };
}

export function toOAuthExchangePayload(code: string): OAuthExchangePayload {
  return {
    code: code.trim(),
  };
}
