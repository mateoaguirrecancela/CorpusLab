export type LoginFormState = {
  email: string;
  password: string;
};

export type LoginResponse = {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  token: string;
};

export type LogoutResponse = {
  message: string;
};
