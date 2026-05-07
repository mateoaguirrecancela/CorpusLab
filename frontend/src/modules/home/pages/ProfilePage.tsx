import { useTranslation } from 'react-i18next';
import { PageContainer } from '@/components/common/PageContainer';
import { PageHeader } from '@/components/common/PageHeader';
import { Spinner } from '@/components/ui/spinner';
import { ProfileDetailsView } from '@/modules/home/components/ProfileDetailsView';
import { ProfileEditForm } from '@/modules/home/components/ProfileEditForm';
import { ProfileUserCard } from '@/modules/home/components/ProfileUserCard';
import { useProfilePage } from '@/modules/home/hooks/useProfilePage';

export default function ProfilePage() {
  const { t, i18n } = useTranslation();
  const {
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
  } = useProfilePage();
  const language = i18n.resolvedLanguage ?? i18n.language ?? 'en';

  return (
    <PageContainer>
      <PageHeader title={t('home.profile.title')} />

      <div className="mt-6 rounded-2xl border border-border bg-surface-base p-4 sm:p-6">
        {isLoading && (
          <div className="rounded-lg border border-border bg-surface-base px-4 py-6 text-sm text-muted-foreground">
            <span className="inline-flex items-center gap-2">
              <Spinner aria-hidden className="size-4" />
              {t('common.loading.profile')}
            </span>
          </div>
        )}

        {!isLoading && profile && (
          <div>
            <ProfileUserCard profile={profile} userInitials={userInitials} />

            {!isEditing && (
              <ProfileDetailsView language={language} onEdit={startEditing} profile={profile} />
            )}

            {isEditing && (
              <ProfileEditForm
                canSave={canSave}
                form={form}
                isSaving={isSaving}
                language={language}
                onCancel={cancelEditing}
                onFieldChange={setField}
                onSave={() => void saveProfile()}
              />
            )}
          </div>
        )}
      </div>
    </PageContainer>
  );
}
