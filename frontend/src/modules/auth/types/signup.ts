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

export type CountryOption = {
  value: string;
  label: string;
};
