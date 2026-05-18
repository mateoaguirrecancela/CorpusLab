import { useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { toast } from 'sonner';
import { PROFILE_QUERY_KEY } from '@/modules/auth/hooks/useProfileQuery';
import { getUpdateProfileErrorMessage, updateProfile } from '@/modules/auth/services/authService';
import { type ProfileFormState, type ProfileResponse } from '@/modules/auth/types/profile';
import { useProfileForm } from '@/modules/home/hooks/useProfileForm';

type ProfileEditorState = Readonly<{
  canSave: boolean;
  fieldErrors: Partial<Record<keyof ProfileFormState, string>>;
  form: ProfileFormState;
  isEditing: boolean;
  isSaving: boolean;
  cancelEditing: () => void;
  saveProfile: () => Promise<void>;
  setField: <K extends keyof ProfileFormState>(field: K, value: ProfileFormState[K]) => void;
  startEditing: () => void;
}>;

export function useProfileEditor(profile: ProfileResponse | undefined): ProfileEditorState {
  const { t } = useTranslation();
  const queryClient = useQueryClient();
  const {
    fieldErrors,
    form,
    getValidValues,
    isValid: isProfileFormValid,
    setField,
    syncWithProfile,
  } = useProfileForm();
  const [isEditing, setIsEditing] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const canSave = isProfileFormValid && !isSaving;

  const startEditing = () => {
    if (!profile) {
      return;
    }

    syncWithProfile(profile);
    setIsEditing(true);
  };

  const cancelEditing = () => {
    if (profile) {
      syncWithProfile(profile);
    }

    setIsEditing(false);
  };

  const saveProfile = async () => {
    if (isSaving) {
      return;
    }

    const values = await getValidValues();
    if (!values) {
      toast.error(t('home.profile.requiredFields'));
      return;
    }

    setIsSaving(true);

    try {
      const updatedProfile = await updateProfile(values);

      queryClient.setQueryData(PROFILE_QUERY_KEY, updatedProfile);
      syncWithProfile(updatedProfile);
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
    fieldErrors,
    form,
    isEditing,
    isSaving,
    saveProfile,
    setField,
    startEditing,
  };
}
