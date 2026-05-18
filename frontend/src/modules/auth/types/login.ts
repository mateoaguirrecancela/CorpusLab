import { type ProfileResponse } from '@/modules/auth/types/profile';

export type LoginFormState = {
  email: string;
  password: string;
};

export type LoginResponse = ProfileResponse & {
  id: number;
  token: string;
};

export type LogoutResponse = {
  message: string;
};
