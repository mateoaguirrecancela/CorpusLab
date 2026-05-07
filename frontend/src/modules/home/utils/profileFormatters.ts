export function formatProfileValue(value: string | null): string {
  if (value === null) {
    return '-';
  }

  const trimmed = value.trim();
  return trimmed.length > 0 ? trimmed : '-';
}

export function formatProfileGender(value: string | null): string {
  if (!value) {
    return '-';
  }

  const normalized = value.toLowerCase();
  return normalized.charAt(0).toUpperCase() + normalized.slice(1);
}

export function formatProfileBirth(value: string | null, language: string): string {
  if (!value) {
    return '-';
  }

  const parsedDate = new Date(value);
  if (Number.isNaN(parsedDate.getTime())) {
    return value;
  }

  return new Intl.DateTimeFormat(language, {
    day: '2-digit',
    month: 'long',
    year: 'numeric',
  }).format(parsedDate);
}
