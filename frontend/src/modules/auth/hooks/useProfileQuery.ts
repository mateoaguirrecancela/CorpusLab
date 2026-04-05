import { useQuery } from '@tanstack/react-query';
import { PROFILE_QUERY_KEY } from '@/modules/auth/constants/queryKeys';
import { getProfile } from '@/modules/auth/services/authService';

export { PROFILE_QUERY_KEY } from '@/modules/auth/constants/queryKeys';

export function useProfileQuery() {
  return useQuery({
    queryKey: PROFILE_QUERY_KEY,
    queryFn: getProfile,
  });
}
