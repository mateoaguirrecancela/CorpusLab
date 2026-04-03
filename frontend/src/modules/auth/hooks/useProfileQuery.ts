import { useQuery } from '@tanstack/react-query';
import { getProfile } from '@/modules/auth/services/authService';

export const PROFILE_QUERY_KEY = ['auth', 'profile'] as const;

export function useProfileQuery() {
  return useQuery({
    queryKey: PROFILE_QUERY_KEY,
    queryFn: getProfile,
  });
}
