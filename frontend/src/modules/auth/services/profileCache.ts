import { type QueryClient } from '@tanstack/react-query';
import { PROFILE_QUERY_KEY } from '@/modules/auth/constants/queryKeys';
import { getProfile } from '@/modules/auth/services/authService';
import { type ProfileResponse } from '@/modules/auth/types/profile';

type BasicUserProfile = {
  email: string;
  firstName: string;
  lastName: string;
};

function buildFallbackProfile(profile: BasicUserProfile): ProfileResponse {
  return {
    email: profile.email,
    firstName: profile.firstName,
    lastName: profile.lastName,
    birth: null,
    gender: null,
    countryCode: null,
    city: null,
  };
}

export async function primeProfileCache(
  queryClient: QueryClient,
  basicProfile: BasicUserProfile,
): Promise<void> {
  try {
    await queryClient.fetchQuery({
      queryKey: PROFILE_QUERY_KEY,
      queryFn: getProfile,
    });
  } catch {
    queryClient.setQueryData<ProfileResponse>(
      PROFILE_QUERY_KEY,
      buildFallbackProfile(basicProfile),
    );
  }
}
