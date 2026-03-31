import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog';
import { FeedbackMessage } from '@/components/ui/feedback-message';
import {
  Field,
  FieldLabel,
} from "@/components/ui/field"
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Spinner } from '@/components/ui/spinner';
import {
  createResearchGroup,
  getCreateGroupErrorMessage,
} from '@/modules/researchgroup/services/researchGroupService';
import { type ResearchGroupSummary } from '@/modules/researchgroup/types/researchGroup';

type CreateResearchGroupDialogProps = {
  trigger: React.ReactNode;
  onCreated: (group: ResearchGroupSummary) => void;
};

export function CreateResearchGroupDialog({
  trigger,
  onCreated,
}: Readonly<CreateResearchGroupDialogProps>) {
  const { t } = useTranslation();
  const [open, setOpen] = useState(false);
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [isSaving, setIsSaving] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  const canSave = name.trim().length > 0 && !isSaving;

  const resetForm = () => {
    setName('');
    setDescription('');
    setErrorMessage('');
    setIsSaving(false);
  };

  const handleOpenChange = (nextOpen: boolean) => {
    if (!nextOpen) {
      resetForm();
    }
    setOpen(nextOpen);
  };

  const handleSubmit = async () => {
    if (!canSave) {
      return;
    }

    setIsSaving(true);
    setErrorMessage('');

    try {
      const created = await createResearchGroup({
        name: name.trim(),
        description: description.trim() || undefined,
      });

      onCreated(created);
      setOpen(false);
      resetForm();
    } catch (error) {
      setErrorMessage(getCreateGroupErrorMessage(error));
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogTrigger render={trigger as React.JSX.Element} />

      <DialogContent className="sm:max-w-xl">
        <DialogHeader>
          <DialogTitle>{t('researchGroup.create.title')}</DialogTitle>
        </DialogHeader>

        <div className="my-8 space-y-4">
          <Field>
            <FieldLabel htmlFor="create-group-name">
              {t('researchGroup.create.nameLabel') + ' *'}
            </FieldLabel>
            <Input
              autoFocus
              id="create-group-name"
              maxLength={256}
              onChange={(e) => setName(e.target.value)}
              placeholder={t('researchGroup.create.namePlaceholder')}
              required
              value={name}
            />
          </Field>

          <Field>
            <FieldLabel htmlFor="create-group-description">
              {t('researchGroup.create.descriptionLabel')}
            </FieldLabel>
            <Textarea
              id="create-group-description"
              maxLength={2048}
              onChange={(e) => setDescription(e.target.value)}
              placeholder={t('researchGroup.create.descriptionPlaceholder')}
              value={description}
            />
          </Field>

          {errorMessage.length > 0 && (
            <FeedbackMessage className="rounded-lg px-4 py-3" message={errorMessage} variant="error" />
          )}
        </div>

        <DialogFooter>
          <Button
            className="h-10 min-w-28 rounded-md bg-[color:var(--cl-primary)] text-sm font-semibold text-white transition-colors hover:bg-[color:var(--cl-primary-deep)] disabled:bg-[color:var(--cl-tertiary)] cursor-pointer"
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
              t('researchGroup.create.submit')
            )}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
