type UserIdentity = {
  firstName?: string | null;
  lastName?: string | null;
  email?: string | null;
};

export function getUserInitials(user: UserIdentity): string {
  const firstInitial = user.firstName?.trim().charAt(0) ?? '';
  const lastInitial = user.lastName?.trim().charAt(0) ?? '';
  const initials = `${firstInitial}${lastInitial}`.trim().toUpperCase();

  if (initials.length > 0) {
    return initials;
  }

  const emailInitial = user.email?.trim().charAt(0).toUpperCase() ?? '';
  return emailInitial.length > 0 ? emailInitial : 'U';
}
