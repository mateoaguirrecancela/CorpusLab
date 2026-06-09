import { type TFunction } from 'i18next';
import { z } from 'zod';
import { isAtLeast16YearsOld } from '@/modules/auth/utils/validation';

export function createProfileSchema(t: TFunction) {
  return z
    .object({
      birth: z
        .string()
        .min(1, t('home.profile.requiredFields'))
        .refine(isAtLeast16YearsOld, t('auth.signup.underAge')),
      city: z
        .string()
        .trim()
        .min(1, t('home.profile.requiredFields'))
        .max(128, t('home.profile.validation.cityMax')),
      countryCode: z.string().length(2, t('home.profile.requiredFields')),
      firstName: z
        .string()
        .trim()
        .min(1, t('home.profile.requiredFields'))
        .max(128, t('home.profile.validation.nameMax')),
      gender: z
        .enum(['', 'MALE', 'FEMALE', 'OTHER'])
        .refine((value) => value.length > 0, t('home.profile.requiredFields')),
      lastName: z
        .string()
        .trim()
        .min(1, t('home.profile.requiredFields'))
        .max(128, t('home.profile.validation.nameMax')),
    })
    .strict();
}

export type ProfileFormValues = z.infer<ReturnType<typeof createProfileSchema>>;
