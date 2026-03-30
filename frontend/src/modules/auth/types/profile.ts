export type ProfileResponse = {
  email: string;
  firstName: string;
  lastName: string;
  birth: string | null;
  gender: string | null;
  countryCode: string | null;
  city: string | null;
};

export type UpdateProfilePayload = {
  firstName: string;
  lastName: string;
  birth?: string;
  gender?: string;
  countryCode?: string;
  city?: string;
};

export type ProfileFormState = {
  firstName: string;
  lastName: string;
  birth: string;
  gender: string;
  countryCode: string;
  city: string;
};
