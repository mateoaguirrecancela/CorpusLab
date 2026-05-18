import { type TFunction } from 'i18next';
import { z } from 'zod';
import { isAtLeast16YearsOld } from '@/modules/auth/utils/validation';

export function createLoginSchema(t: TFunction) {
  return z
    .object({
      email: z
        .string()
        .trim()
        .min(1, t('auth.login.required'))
        .max(254, t('auth.validation.emailMax'))
        .email(t('auth.login.invalidEmail')),
      password: z.string().min(8, t('auth.signup.passwordLength')),
    })
    .strict();
}

export function createSignupSchema(t: TFunction) {
  return z
    .object({
      birth: z
        .string()
        .min(1, t('auth.signup.required'))
        .refine(isAtLeast16YearsOld, t('auth.signup.underAge')),
      city: z
        .string()
        .trim()
        .min(1, t('auth.signup.required'))
        .max(128, t('auth.validation.cityMax')),
      countryCode: z.string().length(2, t('auth.signup.required')),
      email: z
        .string()
        .trim()
        .min(1, t('auth.signup.required'))
        .max(254, t('auth.validation.emailMax'))
        .email(t('auth.signup.invalidEmail')),
      firstName: z
        .string()
        .trim()
        .min(1, t('auth.signup.required'))
        .max(128, t('auth.validation.nameMax')),
      gender: z
        .enum(['', 'MALE', 'FEMALE', 'OTHER'])
        .refine((value) => value.length > 0, t('auth.signup.required')),
      lastName: z
        .string()
        .trim()
        .min(1, t('auth.signup.required'))
        .max(128, t('auth.validation.nameMax')),
      password: z.string().min(8, t('auth.signup.passwordLength')),
    })
    .strict();
}

export function createForgotPasswordSchema(t: TFunction) {
  return z
    .object({
      email: z
        .string()
        .trim()
        .min(1, t('auth.forgotPassword.required'))
        .max(254, t('auth.validation.emailMax'))
        .email(t('auth.login.invalidEmail')),
    })
    .strict();
}

export function createResetPasswordSchema(t: TFunction) {
  return z
    .object({
      confirmPassword: z.string().min(8, t('auth.signup.passwordLength')),
      newPassword: z.string().min(8, t('auth.signup.passwordLength')),
      token: z.string().trim().min(16, t('auth.resetPassword.missingToken')),
    })
    .strict()
    .refine((value) => value.confirmPassword === value.newPassword, {
      message: t('auth.resetPassword.passwordMismatch'),
      path: ['confirmPassword'],
    });
}

export type ForgotPasswordFormValues = z.infer<ReturnType<typeof createForgotPasswordSchema>>;
export type LoginFormValues = z.infer<ReturnType<typeof createLoginSchema>>;
export type ResetPasswordFormValues = z.infer<ReturnType<typeof createResetPasswordSchema>>;
export type SignupFormValues = z.infer<ReturnType<typeof createSignupSchema>>;
