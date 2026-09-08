import { type CountryOption, type RegisterFormState } from '@/modules/auth/types/signup';
import { buildCountryOptions } from '@/shared/constants/countries';

export function getCountryOptions(locale: string): CountryOption[] {
  return buildCountryOptions(locale);
}

type TranslateFn = (key: string) => string;

export function getGenderOptions(t: TranslateFn): CountryOption[] {
  return [
    { value: '', label: t('auth.gender.select') },
    { value: 'MALE', label: t('auth.gender.male') },
    { value: 'FEMALE', label: t('auth.gender.female') },
    { value: 'OTHER', label: t('auth.gender.other') },
  ];
}

export const INITIAL_REGISTER_STATE: RegisterFormState = {
  firstName: '',
  lastName: '',
  email: '',
  password: '',
  birth: '',
  gender: '',
  countryCode: '',
  city: '',
};
