import { type ProfileResponse } from '@/modules/auth/types/profile';
import type { AuthMessageResponse } from '@/modules/auth/types/common';

export type LoginFormState = {
  email: string;
  password: string;
};

export type LoginResponse = ProfileResponse & {
  id: number;
  token: string;
};

export type LogoutResponse = AuthMessageResponse;
