import { useQuery } from '@tanstack/react-query';
import { authQueryKeys } from '@/modules/auth/queryKeys';
import { getProfile } from '@/modules/auth/services/authService';

export function useProfileQuery() {
  return useQuery({
    queryKey: authQueryKeys.profile,
    queryFn: getProfile,
  });
}
