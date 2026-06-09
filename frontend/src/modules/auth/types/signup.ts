export type { CountryOption } from '@/shared/types/country';

export type RegisterFormState = {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
  birth: string;
  gender: '' | 'MALE' | 'FEMALE' | 'OTHER';
  countryCode: string;
  city: string;
};
