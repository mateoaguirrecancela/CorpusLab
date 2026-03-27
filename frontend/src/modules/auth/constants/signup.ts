import { type CountryOption, type RegisterFormState } from '@/modules/auth/types/signup'
import { buildCountryOptions } from '@/lib/countries'

export const COUNTRY_OPTIONS: CountryOption[] = buildCountryOptions()

export const GENDER_OPTIONS: CountryOption[] = [
  { value: '', label: 'Select gender' },
  { value: 'MALE', label: 'Male' },
  { value: 'FEMALE', label: 'Female' },
  { value: 'OTHER', label: 'Other' },
]

export const INITIAL_REGISTER_STATE: RegisterFormState = {
  firstName: '',
  lastName: '',
  email: '',
  password: '',
  birth: '',
  gender: '',
  countryCode: '',
  city: '',
}
