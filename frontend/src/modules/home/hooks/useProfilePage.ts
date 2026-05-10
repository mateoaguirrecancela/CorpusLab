import { useCallback, useEffect, useMemo, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { toast } from 'sonner';
import { getUserInitials } from '@/lib/user';
import { PROFILE_QUERY_KEY, useProfileQuery } from '@/modules/auth/hooks/useProfileQuery';
import {
  getProfileErrorMessage,
  getUpdateProfileErrorMessage,
  updateProfile,
} from '@/modules/auth/services/authService';
import { type ProfileFormState, type ProfileResponse } from '@/modules/auth/types/profile';
import { isAtLeast16YearsOld } from '@/modules/auth/utils/validation';

type ProfilePageState = Readonly<{
  canSave: boolean;
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

function profileToFormState(profile: ProfileResponse): ProfileFormState {
  return {
    firstName: profile.firstName,
    lastName: profile.lastName,
    birth: profile.birth ?? '',
    gender: profile.gender ?? '',
    countryCode: profile.countryCode ?? '',
    city: profile.city ?? '',
  };
}

export function useProfilePage(): ProfilePageState {
  const { t } = useTranslation();
  const queryClient = useQueryClient();
  const {
    data: profile,
    error: profileError,
    isError: isProfileError,
    isLoading,
  } = useProfileQuery();
  const [form, setForm] = useState<ProfileFormState>({
    firstName: '',
    lastName: '',
    birth: '',
    gender: '',
    countryCode: '',
    city: '',
  });
  const [isEditing, setIsEditing] = useState(false);
  const [isSaving, setIsSaving] = useState(false);

  const syncFormWithProfile = useCallback((currentProfile: ProfileResponse) => {
    setForm(profileToFormState(currentProfile));
  }, []);

  const setField = <K extends keyof ProfileFormState>(field: K, value: ProfileFormState[K]) => {
    setForm((current) => ({ ...current, [field]: value }));
  };

  useEffect(() => {
    if (profile && !isEditing) {
      syncFormWithProfile(profile);
    }
  }, [isEditing, profile, syncFormWithProfile]);

  useEffect(() => {
    if (isProfileError) {
      toast.error(getProfileErrorMessage(profileError), { id: 'profile-load-error' });
      return;
    }
  }, [isProfileError, profileError]);

  const userInitials = useMemo(() => {
    if (!profile) {
      return t('common.user').charAt(0);
    }

    return getUserInitials(profile);
  }, [profile, t]);

  const canSave = useMemo(() => {
    const hasRequiredFields =
      form.firstName.trim().length > 0 &&
      form.lastName.trim().length > 0 &&
      isAtLeast16YearsOld(form.birth) &&
      form.gender.trim().length > 0 &&
      form.countryCode.trim().length === 2 &&
      form.city.trim().length > 0;
    return hasRequiredFields && !isSaving;
  }, [form, isSaving]);

  const startEditing = () => {
    if (!profile) {
      return;
    }

    syncFormWithProfile(profile);
    setIsEditing(true);
  };

  const cancelEditing = () => {
    if (profile) {
      syncFormWithProfile(profile);
    }
    setIsEditing(false);
  };

  const saveProfile = async () => {
    if (!canSave) {
      toast.error(t('home.profile.requiredFields'));
      return;
    }

    setIsSaving(true);

    try {
      const updatedProfile = await updateProfile({
        firstName: form.firstName,
        lastName: form.lastName,
        birth: form.birth,
        gender: form.gender,
        countryCode: form.countryCode,
        city: form.city,
      });

      queryClient.setQueryData(PROFILE_QUERY_KEY, updatedProfile);
      syncFormWithProfile(updatedProfile);
      setIsEditing(false);
      toast.success(t('home.profile.updated'));
    } catch (error) {
      toast.error(getUpdateProfileErrorMessage(error));
    } finally {
      setIsSaving(false);
    }
  };

  return {
    canSave,
    cancelEditing,
    form,
    isEditing,
    isLoading,
    isSaving,
    profile,
    saveProfile,
    setField,
    startEditing,
    userInitials,
  };
}
