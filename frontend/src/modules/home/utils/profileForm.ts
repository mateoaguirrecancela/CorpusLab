import { type FieldErrors } from 'react-hook-form';
import { mergeFormValues, pickFieldErrors } from '@/lib/formUtils';
import { type ProfileResponse } from '@/modules/auth/types/profile';
import { type ProfileFormValues } from '@/modules/home/schemas/profileFormSchemas';

const PROFILE_GENDERS = ['MALE', 'FEMALE', 'OTHER'] as const;

export const EMPTY_PROFILE_FORM: ProfileFormValues = {
  birth: '',
  city: '',
  countryCode: '',
  firstName: '',
  gender: '',
  lastName: '',
};

export const PROFILE_FORM_FIELDS = [
  'birth',
  'city',
  'countryCode',
  'firstName',
  'gender',
  'lastName',
] as const satisfies readonly (keyof ProfileFormValues)[];

export function profileToFormState(profile: ProfileResponse): ProfileFormValues {
  return {
    firstName: profile.firstName,
    lastName: profile.lastName,
    birth: profile.birth ?? '',
    gender: normalizeProfileGender(profile.gender),
    countryCode: profile.countryCode ?? '',
    city: profile.city ?? '',
  };
}

export function mergeProfileFormValues(
  watchedValues: Partial<ProfileFormValues>,
): ProfileFormValues {
  return mergeFormValues(EMPTY_PROFILE_FORM, watchedValues);
}

export function pickProfileFieldErrors(
  errors: FieldErrors<ProfileFormValues>,
): Partial<Record<keyof ProfileFormValues, string>> {
  return pickFieldErrors(errors, PROFILE_FORM_FIELDS);
}

function normalizeProfileGender(gender: string | null): ProfileFormValues['gender'] {
  return PROFILE_GENDERS.find((validGender) => validGender === gender) ?? '';
}
