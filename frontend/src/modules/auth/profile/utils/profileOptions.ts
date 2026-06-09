import { type TFunction } from 'i18next';

type ProfileOption = {
  value: string;
  label: string;
};

export function getProfileGenderOptions(t: TFunction): ProfileOption[] {
  return [
    { value: '', label: t('auth.gender.select') },
    { value: 'MALE', label: t('auth.gender.male') },
    { value: 'FEMALE', label: t('auth.gender.female') },
    { value: 'OTHER', label: t('auth.gender.other') },
  ];
}
