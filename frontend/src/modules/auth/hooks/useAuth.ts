import { useQuery } from '@tanstack/react-query';
import { PROFILE_QUERY_KEY } from '@/modules/auth/constants/queryKeys';
import { getProfile } from '@/modules/auth/services/authService';
import { hasSessionToken } from '@/modules/auth/services/sessionService';

type UseAuthResult = {
  isAuthenticated: boolean;
  isLoading: boolean;
};

export function useAuth(): UseAuthResult {
  const hasToken = hasSessionToken();

  const { data, isLoading, isFetching, isError } = useQuery({
    queryKey: PROFILE_QUERY_KEY,
    queryFn: getProfile,
    enabled: hasToken,
    retry: false,
  });

  return {
    isAuthenticated: hasToken && !isError && Boolean(data),
    isLoading: hasToken && (isLoading || isFetching),
  };
}
