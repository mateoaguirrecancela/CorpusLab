import { useCallback } from 'react';
import { type OAuthProvider } from '@/modules/auth/constants/session';
import { redirectToOAuthAuthorization } from '@/modules/auth/services/authService';

export function useOAuthAuthorization(isDisabled: boolean) {
  return useCallback(
    (provider: OAuthProvider) => {
      if (isDisabled) {
        return;
      }

      redirectToOAuthAuthorization(provider);
    },
    [isDisabled],
  );
}
