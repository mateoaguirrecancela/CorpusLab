import { useCallback, useMemo } from 'react';
import { zodResolver } from '@hookform/resolvers/zod';
import { type Path, type PathValue, useForm, useWatch } from 'react-hook-form';
import { useTranslation } from 'react-i18next';
import { type ProfileFormState, type ProfileResponse } from '@/modules/auth/types/profile';
import {
  createProfileSchema,
  type ProfileFormValues,
} from '@/modules/home/schemas/profileFormSchemas';
import {
  EMPTY_PROFILE_FORM,
  mergeProfileFormValues,
  pickProfileFieldErrors,
  profileToFormState,
} from '@/modules/home/utils/profileForm';

export function useProfileForm() {
  const { t } = useTranslation();
  const profileSchema = useMemo(() => createProfileSchema(t), [t]);
  const profileForm = useForm<ProfileFormValues>({
    defaultValues: EMPTY_PROFILE_FORM,
    mode: 'onChange',
    resolver: zodResolver(profileSchema),
  });
  const {
    control,
    formState: { errors, isValid },
    getValues,
    reset,
    setValue,
    trigger,
  } = profileForm;
  const watchedForm = useWatch({ control });

  const form: ProfileFormState = mergeProfileFormValues(watchedForm);
  const fieldErrors = pickProfileFieldErrors(errors);

  const syncWithProfile = useCallback(
    (profile: ProfileResponse) => {
      reset(profileToFormState(profile));
      void trigger();
    },
    [reset, trigger],
  );

  const setField = <K extends keyof ProfileFormState>(field: K, value: ProfileFormState[K]) => {
    const fieldPath = field as Path<ProfileFormValues>;
    setValue(fieldPath, value as PathValue<ProfileFormValues, typeof fieldPath>, {
      shouldDirty: true,
      shouldValidate: true,
    });
  };

  const getValidValues = async (): Promise<ProfileFormValues | null> => {
    const isFormValid = await trigger();
    if (!isFormValid) {
      return null;
    }

    return profileSchema.parse(getValues());
  };

  return {
    fieldErrors,
    form,
    getValidValues,
    isValid,
    setField,
    syncWithProfile,
  };
}
