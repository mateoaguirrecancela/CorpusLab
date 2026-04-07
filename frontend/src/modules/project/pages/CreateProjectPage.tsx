import { type ReactNode, useEffect, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate, useSearchParams } from 'react-router';
import { toast } from 'sonner';
import { BackButton } from '@/components/common/BackButton';
import { FormFieldControl } from '@/components/common/FormFieldControl';
import { PageContainer } from '@/components/common/PageContainer';
import { Button } from '@/components/ui/button';
import { Spinner } from '@/components/ui/spinner';
import { useCreateProjectMutation } from '@/modules/project/hooks/useProjectQueries';
import { getCreateProjectErrorMessage } from '@/modules/project/services/projectService';
import { useResearchGroupsQuery } from '@/modules/researchgroup/hooks/useResearchGroupQueries';

export default function CreateProjectPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const { data: groups = [], isLoading: isLoadingGroups } = useResearchGroupsQuery();
  const createProjectMutation = useCreateProjectMutation();

  const manageableGroups = useMemo(
    () => groups.filter((group) => group.role === 'OWNER' || group.role === 'ADMIN'),
    [groups],
  );

  const initialGroupId = Number(searchParams.get('groupId'));
  const hasInitialGroupId = Number.isFinite(initialGroupId) && initialGroupId > 0;

  const lockedGroup = useMemo(
    () => manageableGroups.find((group) => group.id === initialGroupId),
    [manageableGroups, initialGroupId],
  );

  const isGroupLocked = hasInitialGroupId && Boolean(lockedGroup);

  const [selectedGroupId, setSelectedGroupId] = useState('');
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');

  useEffect(() => {
    if (isGroupLocked && lockedGroup) {
      setSelectedGroupId(String(lockedGroup.id));
      return;
    }

    if (selectedGroupId.length === 0 && manageableGroups.length > 0) {
      setSelectedGroupId(String(manageableGroups[0].id));
    }
  }, [isGroupLocked, lockedGroup, manageableGroups, selectedGroupId]);

  const numericGroupId = Number(selectedGroupId);
  const isSaving = createProjectMutation.isPending;
  const canSave =
    Number.isFinite(numericGroupId) && numericGroupId > 0 && name.trim().length > 0 && !isSaving;

  let content: ReactNode;
  if (isLoadingGroups) {
    content = (
      <div className="mt-8 rounded-md border border-border bg-background px-4 py-6 text-sm text-muted-foreground">
        <span className="inline-flex items-center gap-2">
          <Spinner aria-hidden className="size-4" />
          {t('project.create.loadingGroups')}
        </span>
      </div>
    );
  } else if (manageableGroups.length === 0) {
    content = (
      <p className="mt-8 rounded-md border border-dashed border-border bg-background px-4 py-5 text-sm text-muted-foreground">
        {t('project.create.noGroups')}
      </p>
    );
  } else {
    content = (
      <div className="mt-8 space-y-4">
        <FormFieldControl
          controlType="select"
          id="create-project-group"
          label={t('project.create.groupLabel')}
          onValueChange={setSelectedGroupId}
          options={manageableGroups.map((group) => ({
            label: group.name,
            value: String(group.id),
          }))}
          required
          selectProps={{ disabled: isGroupLocked, required: true }}
          value={selectedGroupId}
        />

        <FormFieldControl
          id="create-project-name-page"
          inputProps={{
            maxLength: 256,
            placeholder: t('project.create.namePlaceholder'),
            required: true,
          }}
          label={t('project.create.nameLabel')}
          onValueChange={setName}
          required
          value={name}
        />

        <FormFieldControl
          controlType="textarea"
          id="create-project-description-page"
          label={t('project.create.descriptionLabel')}
          onValueChange={setDescription}
          textareaProps={{
            maxLength: 2048,
            placeholder: t('project.create.descriptionPlaceholder'),
          }}
          value={description}
        />

        <div className="flex justify-end">
          <Button
            className="h-10 min-w-36 rounded-md bg-primary text-sm font-semibold text-white transition-colors hover:bg-primary-strong disabled:bg-secondary cursor-pointer"
            disabled={!canSave}
            onClick={() => void handleSubmit()}
            type="button"
          >
            {isSaving ? (
              <span className="inline-flex items-center gap-2">
                <Spinner aria-hidden className="size-4" />
                {t('common.actions.saving')}
              </span>
            ) : (
              t('project.create.submit')
            )}
          </Button>
        </div>
      </div>
    );
  }

  const handleSubmit = async () => {
    if (!canSave) {
      return;
    }

    try {
      await createProjectMutation.mutateAsync({
        groupId: numericGroupId,
        payload: {
          name: name.trim(),
          description: description.trim() || undefined,
        },
      });

      toast.success(t('project.create.success'));
      navigate(`/home/research-groups/${numericGroupId}`);
    } catch (error) {
      toast.error(getCreateProjectErrorMessage(error));
    }
  };

  return (
    <PageContainer className="py-4 sm:py-6">
      <div className="space-y-4">
        <div className="flex items-center justify-between gap-3">
          <BackButton fallbackTo="/home/research-groups" />
        </div>

        <section className="rounded-md border border-border bg-surface-base p-6 sm:p-7">
          <h1 className="text-3xl font-black tracking-tight text-primary sm:text-4xl">
            {t('project.create.pageTitle')}
          </h1>
          <p className="mt-2 text-sm text-muted-foreground">
            {t('project.create.pageDescription')}
          </p>

          {content}
        </section>
      </div>
    </PageContainer>
  );
}
