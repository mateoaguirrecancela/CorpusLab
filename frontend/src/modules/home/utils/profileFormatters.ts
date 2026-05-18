import { type TFunction } from 'i18next';
import { formatDate } from '@/lib/dateFormatters';

const PROFILE_GENDER_LABEL_KEYS: Record<string, string> = {
  FEMALE: 'auth.gender.female',
  MALE: 'auth.gender.male',
  OTHER: 'auth.gender.other',
};

export function formatProfileValue(value: string | null): string {
  if (value === null) {
    return '-';
  }

  const trimmed = value.trim();
  return trimmed.length > 0 ? trimmed : '-';
}

export function formatProfileGender(value: string | null, t: TFunction): string {
  if (!value) {
    return '-';
  }

  const labelKey = PROFILE_GENDER_LABEL_KEYS[value];
  return labelKey ? t(labelKey) : formatProfileValue(value);
}

export function formatProfileBirth(value: string | null, language: string): string {
  if (!value) {
    return '-';
  }

  return formatDate(value, language, {
    day: '2-digit',
    fallback: value,
    month: 'long',
    year: 'numeric',
  });
}
