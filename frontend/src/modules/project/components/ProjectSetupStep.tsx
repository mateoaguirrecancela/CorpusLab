import { useCallback, useEffect, useMemo, useState } from 'react';
import { FileText, FileUp, Plus, X } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { toast } from 'sonner';
import { FormFieldControl } from '@/components/common/FormFieldControl';
import { Button } from '@/components/ui/button';
import { Spinner } from '@/components/ui/spinner';
import { Textarea } from '@/components/ui/textarea';
import { LabelEditorDialog } from '@/modules/project/components/LabelEditorDialog';
import { LabelRow } from '@/modules/project/components/LabelRow';
import { UploadDropzone } from '@/modules/project/components/UploadDropzone';
import { LABEL_COLOR_PALETTE } from '@/modules/project/constants/labelColorPalette';
import {
  type ConfigureProjectSetupPayload,
  type DatasetItem,
  type ProjectSetupLabel,
  type ProjectType,
} from '@/modules/project/types/project';
import { isNerCompatibleDataset } from '@/modules/project/utils/projectUtils';

function formatFileSize(bytes: number): string {
  if (bytes < 1024) {
    return `${bytes} B`;
  }
  if (bytes < 1024 * 1024) {
    return `${(bytes / 1024).toFixed(1)} KB`;
  }
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

function normalizeLabelsForProjectType(
  labels: ProjectSetupLabel[],
  nextType: ProjectType,
): ProjectSetupLabel[] {
  if (nextType === 'SEQ2SEQ') {
    return [];
  }

  if (nextType === 'NER') {
    return labels.map((label) => ({
      ...label,
      color: label.color ?? LABEL_COLOR_PALETTE[0],
    }));
  }

  return labels.map((label) => ({ ...label, color: null }));
}

function hasDuplicateLabelName(
  labels: ProjectSetupLabel[],
  normalizedName: string,
  mode: 'create' | 'edit',
  editingLabelIndex: number | null,
): boolean {
  return labels.some((label, index) => {
    if (mode === 'edit' && editingLabelIndex === index) {
      return false;
    }

    return label.name.toLowerCase() === normalizedName.toLowerCase();
  });
}

function createLabelCandidate(
  normalizedName: string,
  draftLabelColor: string,
  isNerProjectType: boolean,
): ProjectSetupLabel {
  return {
    name: normalizedName,
    color: isNerProjectType ? draftLabelColor : null,
  };
}

async function readFileAsBase64(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();

    reader.onload = () => {
      const result = reader.result;
      if (typeof result !== 'string') {
        reject(new Error('Invalid file content'));
        return;
      }

      const commaIndex = result.indexOf(',');
      resolve(commaIndex >= 0 ? result.slice(commaIndex + 1) : result);
    };

    reader.onerror = () => reject(new Error('Could not read file'));
    reader.readAsDataURL(file);
  });
}

type ProjectSetupStepProps = Readonly<{
  datasetItems: Array<Pick<DatasetItem, 'mimeType' | 'fileName'>>;
  onBack: () => void;
  onCompleted: (payload: ConfigureProjectSetupPayload) => void | Promise<void>;
}>;

export function ProjectSetupStep({ datasetItems, onBack, onCompleted }: ProjectSetupStepProps) {
  const { t } = useTranslation();

  const [projectType, setProjectType] = useState<ProjectType>('TEXT_CLASSIFICATION_SIMPLE');
  const [labels, setLabels] = useState<ProjectSetupLabel[]>([]);
  const [guidelineMode, setGuidelineMode] = useState<'TEXT' | 'PDF'>('TEXT');
  const [guidelineText, setGuidelineText] = useState('');
  const [guidelinePdfFile, setGuidelinePdfFile] = useState<File | null>(null);
  const [isLabelEditorOpen, setIsLabelEditorOpen] = useState(false);
  const [labelDialogMode, setLabelDialogMode] = useState<'create' | 'edit'>('create');
  const [editingLabelIndex, setEditingLabelIndex] = useState<number | null>(null);
  const [draftLabelName, setDraftLabelName] = useState('');
  const [draftLabelColor, setDraftLabelColor] = useState(LABEL_COLOR_PALETTE[0]);
  const [isPreparingSetup, setIsPreparingSetup] = useState(false);

  const isNerDatasetCompatible = useMemo(
    () => isNerCompatibleDataset(datasetItems),
    [datasetItems],
  );
  const isNerProjectType = projectType === 'NER';
  const requiresLabels = projectType !== 'SEQ2SEQ';
  const isLabelNameValid = draftLabelName.trim().length > 0;
  const isLabelColorValid = !isNerProjectType || draftLabelColor.trim().length > 0;
  const isLabelDialogSaveDisabled = !isLabelNameValid || !isLabelColorValid;

  const applyProjectType = useCallback((nextType: ProjectType) => {
    setProjectType(nextType);

    setLabels((prev) => normalizeLabelsForProjectType(prev, nextType));
  }, []);

  const handleProjectTypeChange = useCallback(
    (nextType: ProjectType) => {
      if (nextType === 'NER' && isNerDatasetCompatible === false) {
        toast.error(t('project.create.nerDatasetIncompatibleError'));
        return;
      }

      applyProjectType(nextType);
    },
    [applyProjectType, isNerDatasetCompatible, t],
  );

  useEffect(() => {
    if (projectType === 'NER' && isNerDatasetCompatible === false) {
      applyProjectType('TEXT_CLASSIFICATION_SIMPLE');
    }
  }, [applyProjectType, isNerDatasetCompatible, projectType]);

  const hasValidGuideline =
    guidelineMode === 'TEXT' ? guidelineText.trim().length > 0 : guidelinePdfFile !== null;
  const canSaveSetup =
    hasValidGuideline && (!requiresLabels || labels.length > 0) && !isPreparingSetup;

  const handleGuidelinePdfSelected = (files: FileList | null) => {
    if (!files || files.length === 0) {
      return;
    }

    setGuidelinePdfFile(files[0]);
    setGuidelineText('');
  };

  const openCreateLabelDialog = () => {
    setLabelDialogMode('create');
    setEditingLabelIndex(null);
    setDraftLabelName('');
    setDraftLabelColor(LABEL_COLOR_PALETTE[0]);
    setIsLabelEditorOpen(true);
  };

  const openEditLabelDialog = (index: number) => {
    const target = labels[index];
    if (!target) {
      return;
    }

    setLabelDialogMode('edit');
    setEditingLabelIndex(index);
    setDraftLabelName(target.name);
    setDraftLabelColor(target.color ?? LABEL_COLOR_PALETTE[0]);
    setIsLabelEditorOpen(true);
  };

  const removeLabelAt = (index: number) => {
    setLabels((prev) => prev.filter((_, idx) => idx !== index));
  };

  const saveLabelFromDialog = () => {
    const normalizedName = draftLabelName.trim();
    if (normalizedName.length === 0) {
      return;
    }

    const duplicated = hasDuplicateLabelName(
      labels,
      normalizedName,
      labelDialogMode,
      editingLabelIndex,
    );

    if (duplicated) {
      toast.error(t('project.create.labelsDuplicateError'));
      return;
    }

    const candidate = createLabelCandidate(normalizedName, draftLabelColor, isNerProjectType);

    if (labelDialogMode === 'create') {
      setLabels((prev) => [...prev, candidate]);
    } else if (editingLabelIndex !== null) {
      setLabels((prev) =>
        prev.map((label, index) => (index === editingLabelIndex ? candidate : label)),
      );
    }

    setIsLabelEditorOpen(false);
  };

  const handleSaveProjectSetup = async () => {
    if (!canSaveSetup || isPreparingSetup) {
      return;
    }

    setIsPreparingSetup(true);

    let guidelinePdfBase64: string | undefined;

    if (guidelineMode === 'PDF' && guidelinePdfFile) {
      try {
        guidelinePdfBase64 = await readFileAsBase64(guidelinePdfFile);
      } catch {
        setIsPreparingSetup(false);
        toast.error(t('project.create.guidelinePdfReadError'));
        return;
      }
    }

    try {
      await onCompleted({
        projectType,
        labels,
        guidelineText: guidelineMode === 'TEXT' ? guidelineText.trim() : undefined,
        guidelinePdfBase64,
      });
    } finally {
      setIsPreparingSetup(false);
    }
  };

  return (
    <div className="mt-8 space-y-6">
      <FormFieldControl
        controlType="select"
        id="create-project-type"
        label={t('project.create.projectTypeLabel')}
        message={
          isNerDatasetCompatible === false
            ? t('project.create.nerDatasetIncompatibleHint')
            : undefined
        }
        onValueChange={(value) => handleProjectTypeChange(value as ProjectType)}
        options={[
          {
            label: t('project.create.projectTypes.textClassificationSimple'),
            value: 'TEXT_CLASSIFICATION_SIMPLE',
          },
          {
            label: t('project.create.projectTypes.textClassificationMultiLabel'),
            value: 'TEXT_CLASSIFICATION_MULTILABEL',
          },
          {
            label: t('project.create.projectTypes.ner'),
            disabled: isNerDatasetCompatible === false,
            value: 'NER',
          },
          {
            label: t('project.create.projectTypes.seq2seq'),
            value: 'SEQ2SEQ',
          },
        ]}
        required
        selectProps={{ required: true }}
        value={projectType}
      />

      {requiresLabels && (
        <section>
          <div className="flex items-end justify-between gap-3">
            <p className="text-xs font-bold uppercase tracking-wider text-muted-foreground">
              {t('project.create.labelsSectionTitle')} *
            </p>

            <Button
              className="h-10 rounded-md bg-primary px-3 text-sm font-semibold text-white hover:bg-primary-strong cursor-pointer"
              onClick={openCreateLabelDialog}
              type="button"
            >
              <Plus className="mr-1 size-4" />
              {t('project.create.addLabel')}
            </Button>
          </div>

          <div className="mt-4">
            {labels.length === 0 ? (
              <p className="rounded-lg border border-dashed border-border bg-background px-4 py-5 text-sm text-muted-foreground">
                {t('project.create.labelsEmptyState')}
              </p>
            ) : (
              <div className="space-y-2">
                {labels.map((label, index) => (
                  <LabelRow
                    editLabelText={t('project.create.editLabel')}
                    isNerProjectType={isNerProjectType}
                    key={`${label.name}-${index}`}
                    label={label}
                    onEdit={() => openEditLabelDialog(index)}
                    onRemove={() => removeLabelAt(index)}
                    removeLabelText={t('project.create.removeLabel')}
                  />
                ))}
              </div>
            )}
          </div>
        </section>
      )}

      <section className="space-y-3">
        <p className="text-xs font-bold uppercase tracking-wider text-muted-foreground">
          {t('project.create.guidelineSectionTitle')}
        </p>

        <div className="overflow-hidden rounded-lg border border-primary/20 bg-surface-base">
          <div className="flex border-b border-primary/20 bg-primary/5">
            <button
              className={[
                'inline-flex items-center justify-center gap-2 border-r border-primary/20 px-4 py-3 text-xs font-bold tracking-wider uppercase transition-colors sm:justify-start sm:px-6',
                guidelineMode === 'TEXT'
                  ? 'bg-surface-base text-primary shadow-[inset_0_-2px_0_0] shadow-primary'
                  : 'text-muted-foreground hover:bg-accent/30',
              ].join(' ')}
              onClick={() => {
                setGuidelineMode('TEXT');
                setGuidelinePdfFile(null);
              }}
              type="button"
            >
              <FileText className="size-3.5 text-muted-foreground" />
              {t('project.create.guidelineWriteTab')}
            </button>
            <button
              className={[
                'inline-flex items-center justify-center gap-2 px-4 py-3 text-xs font-bold tracking-wider uppercase transition-colors sm:justify-start sm:px-6',
                guidelineMode === 'PDF'
                  ? 'bg-surface-base text-primary shadow-[inset_0_-2px_0_0] shadow-primary'
                  : 'text-muted-foreground hover:bg-accent/30',
              ].join(' ')}
              onClick={() => {
                setGuidelineMode('PDF');
                setGuidelineText('');
              }}
              type="button"
            >
              <FileUp className="size-3.5 text-muted-foreground" />
              {t('project.create.guidelineUploadTab')}
            </button>
          </div>

          <div className="bg-slate-50/60 p-4 sm:p-6">
            {guidelineMode === 'TEXT' ? (
              <Textarea
                id="project-guideline-text"
                maxLength={5000}
                onChange={(event) => setGuidelineText(event.target.value)}
                placeholder={t('project.create.guidelineTextPlaceholder')}
                required
                value={guidelineText}
              />
            ) : (
              <>
                <UploadDropzone
                  accept=".pdf,application/pdf"
                  description={t('project.create.guidelinePdfHint')}
                  onFilesChange={handleGuidelinePdfSelected}
                  title={t('project.create.guidelineUploadTitle')}
                />

                {guidelinePdfFile && (
                  <div className="mt-4 flex items-center justify-between gap-3 rounded-lg border border-border bg-background px-4 py-3">
                    <div className="min-w-0">
                      <p className="truncate text-sm font-semibold text-primary">
                        {guidelinePdfFile.name}
                      </p>
                      <p className="text-xs text-muted-foreground">
                        {formatFileSize(guidelinePdfFile.size)}
                      </p>
                    </div>
                    <button
                      aria-label={t('project.create.removeFile')}
                      className="inline-flex size-8 items-center justify-center rounded-md text-muted-foreground transition-colors hover:bg-accent hover:text-primary"
                      onClick={() => setGuidelinePdfFile(null)}
                      type="button"
                    >
                      <X className="size-4" />
                    </button>
                  </div>
                )}
              </>
            )}
          </div>
        </div>
      </section>

      <div className="flex justify-end gap-3">
        <Button
          className="h-10 rounded-md border border-border bg-surface-base px-6 text-sm font-semibold text-primary hover:bg-accent cursor-pointer"
          onClick={onBack}
          type="button"
          variant="outline"
        >
          {t('project.create.previousStepSimple')}
        </Button>
        <Button
          className="h-10 min-w-44 rounded-md bg-primary text-sm font-semibold text-white transition-colors hover:bg-primary-strong disabled:bg-secondary cursor-pointer"
          disabled={!canSaveSetup}
          onClick={() => void handleSaveProjectSetup()}
          type="button"
        >
          {isPreparingSetup ? (
            <span className="inline-flex items-center gap-2">
              <Spinner aria-hidden className="size-4" />
              {t('project.create.savingSetup')}
            </span>
          ) : (
            t('project.create.nextStepSimple')
          )}
        </Button>
      </div>

      <LabelEditorDialog
        colorLabel={t('project.create.labelColor')}
        currentColor={draftLabelColor}
        currentName={draftLabelName}
        inputLabel={t('project.create.labelName')}
        isNerProjectType={isNerProjectType}
        isOpen={isLabelEditorOpen}
        mode={labelDialogMode}
        onColorChange={setDraftLabelColor}
        onNameChange={setDraftLabelName}
        onOpenChange={setIsLabelEditorOpen}
        onSave={saveLabelFromDialog}
        isSaveDisabled={isLabelDialogSaveDisabled}
        placeholder={t('project.create.labelNamePlaceholder')}
        saveCreateText={t('project.create.addLabel')}
        saveEditText={t('project.create.saveLabel')}
        titleCreate={t('project.create.createLabelTitle')}
        titleEdit={t('project.create.editLabelTitle')}
      />
    </div>
  );
}
