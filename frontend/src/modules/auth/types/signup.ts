export type RegisterFormState = {
  firstName: string
  lastName: string
  email: string
  password: string
  birth: string
  gender: '' | 'MALE' | 'FEMALE' | 'OTHER'
  countryCode: string
  city: string
}

export type RegisterPayload = RegisterFormState

export type RegisterResponse = {
  id: number
  email: string
  firstName: string
  lastName: string
  createdAt: string
}

export type CountryOption = {
  value: string
  label: string
}
