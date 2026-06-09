import { useTranslation } from 'react-i18next';
import { useToastMessages } from '@/shared/hooks/useToastMessages';
import { getUserInitials } from '@/shared/utils/user';
import { useProfileQuery } from '@/modules/auth/hooks/useProfileQuery';
import { getProfileErrorMessage } from '@/modules/auth/services/authService';
import { type ProfileFormState, type ProfileResponse } from '@/modules/auth/types/profile';
import { useProfileEditor } from '@/modules/auth/profile/hooks/useProfileEditor';

type ProfilePageState = Readonly<{
  canSave: boolean;
  fieldErrors: Partial<Record<keyof ProfileFormState, string>>;
  form: ProfileFormState;
  isEditing: boolean;
  isLoading: boolean;
  isSaving: boolean;
  profile: ProfileResponse | undefined;
  userInitials: string;
  cancelEditing: () => void;
  saveProfile: () => Promise<void>;
  setField: <K extends keyof ProfileFormState>(field: K, value: ProfileFormState[K]) => void;
  startEditing: () => void;
}>;

export function useProfilePage(): ProfilePageState {
  const { t } = useTranslation();
  const {
    data: profile,
    error: profileError,
    isError: isProfileError,
    isLoading,
  } = useProfileQuery();
  const editor = useProfileEditor(profile);
  const profileLoadErrorMessage = isProfileError ? getProfileErrorMessage(profileError) : '';
  const userInitials = profile ? getUserInitials(profile) : t('common.user').charAt(0);

  useToastMessages({
    errorMessage: profileLoadErrorMessage,
    errorToastId: 'profile-load-error',
  });

  return {
    ...editor,
    isLoading,
    profile,
    userInitials,
  };
}
