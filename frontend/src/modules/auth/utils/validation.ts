const ISO_DATE_ONLY_PATTERN = /^(\d{4})-(\d{2})-(\d{2})$/;

export function isAtLeast16YearsOld(birthDate: string): boolean {
  const parsedBirthDate = parseIsoDateOnly(birthDate);
  if (!parsedBirthDate) {
    return false;
  }

  const latestAllowedBirthDate = new Date();
  latestAllowedBirthDate.setFullYear(latestAllowedBirthDate.getFullYear() - 16);

  return parsedBirthDate <= latestAllowedBirthDate;
}

function parseIsoDateOnly(value: string): Date | null {
  const match = ISO_DATE_ONLY_PATTERN.exec(value);
  if (!match) {
    return null;
  }

  const [, yearText, monthText, dayText] = match;
  const year = Number(yearText);
  const monthIndex = Number(monthText) - 1;
  const day = Number(dayText);
  const date = new Date(year, monthIndex, day);

  if (date.getFullYear() !== year || date.getMonth() !== monthIndex || date.getDate() !== day) {
    return null;
  }

  return date;
}
