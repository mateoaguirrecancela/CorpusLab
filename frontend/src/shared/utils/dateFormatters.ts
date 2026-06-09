type DateFormatOptions = Intl.DateTimeFormatOptions & {
  fallback?: string;
};

export function formatDate(
  value: string | null | undefined,
  locale: string,
  { fallback = '-', ...formatOptions }: DateFormatOptions,
): string {
  if (!value) {
    return fallback;
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return fallback;
  }

  return new Intl.DateTimeFormat(locale, formatOptions).format(date);
}
