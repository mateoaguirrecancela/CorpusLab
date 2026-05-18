import { useTranslation } from 'react-i18next';
import { PageContainer } from '@/components/common/PageContainer';
import { PageHeader } from '@/components/common/PageHeader';
import { Spinner } from '@/components/ui/spinner';
import { ProfileDetailsView } from '@/modules/home/components/ProfileDetailsView';
import { ProfileEditForm } from '@/modules/home/components/ProfileEditForm';
import { ProfileUserCard } from '@/modules/home/components/ProfileUserCard';
import { useProfilePage } from '@/modules/home/hooks/useProfilePage';
import { type ProfileFormState, type ProfileResponse } from '@/modules/auth/types/profile';

type ProfileContentProps = Readonly<{
  canSave: boolean;
  fieldErrors: Partial<Record<keyof ProfileFormState, string>>;
  form: ProfileFormState;
  isEditing: boolean;
  isLoading: boolean;
  isSaving: boolean;
  profile: ProfileResponse | undefined;
  userInitials: string;
  onCancelEditing: () => void;
  onFieldChange: <K extends keyof ProfileFormState>(field: K, value: ProfileFormState[K]) => void;
  onSaveProfile: () => Promise<void>;
  onStartEditing: () => void;
}>;

function ProfileLoadingState() {
  const { t } = useTranslation();

  return (
    <div className="rounded-lg border border-border bg-surface-base px-4 py-6 text-sm text-muted-foreground">
      <span className="inline-flex items-center gap-2">
        <Spinner aria-hidden className="size-4" />
        {t('common.loading.profile')}
      </span>
    </div>
  );
}

function ProfileContent({
  canSave,
  fieldErrors,
  form,
  isEditing,
  isLoading,
  isSaving,
  profile,
  userInitials,
  onCancelEditing,
  onFieldChange,
  onSaveProfile,
  onStartEditing,
}: ProfileContentProps) {
  if (isLoading) {
    return <ProfileLoadingState />;
  }

  if (!profile) {
    return null;
  }

  return (
    <div>
      <ProfileUserCard profile={profile} userInitials={userInitials} />

      {!isEditing && <ProfileDetailsView onEdit={onStartEditing} profile={profile} />}

      {isEditing && (
        <ProfileEditForm
          canSave={canSave}
          fieldErrors={fieldErrors}
          form={form}
          isSaving={isSaving}
          onCancel={onCancelEditing}
          onFieldChange={onFieldChange}
          onSave={() => void onSaveProfile()}
        />
      )}
    </div>
  );
}

export default function ProfilePage() {
  const { t } = useTranslation();
  const {
    canSave,
    cancelEditing,
    fieldErrors,
    form,
    isEditing,
    isLoading,
    isSaving,
    profile,
    saveProfile,
    setField,
    startEditing,
    userInitials,
  } = useProfilePage();

  return (
    <PageContainer>
      <PageHeader title={t('home.profile.title')} />

      <div className="mt-6 rounded-2xl border border-border bg-surface-base p-4 sm:p-6">
        <ProfileContent
          canSave={canSave}
          fieldErrors={fieldErrors}
          form={form}
          isEditing={isEditing}
          isLoading={isLoading}
          isSaving={isSaving}
          profile={profile}
          userInitials={userInitials}
          onCancelEditing={cancelEditing}
          onFieldChange={setField}
          onSaveProfile={saveProfile}
          onStartEditing={startEditing}
        />
      </div>
    </PageContainer>
  );
}
