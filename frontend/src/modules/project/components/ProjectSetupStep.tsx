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
  type ProjectSetupLabel,
  type ProjectType,
} from '@/modules/project/types/project';
import { isNerCompatibleDataset } from '@/modules/project/utils/projectUtils';
import { ProjectTypeSelector } from '@/modules/project/components/ProjectTypeSelector';

const MAX_GUIDELINE_SIZE_MB = 10;

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

function isCsvDatasetFile(file: File): boolean {
  const normalizedMimeType = file.type.toLowerCase();
  const normalizedFileName = file.name.toLowerCase();

  return normalizedMimeType.includes('csv') || normalizedFileName.endsWith('.csv');
}

function removeUtf8Bom(value: string): string {
  return value.startsWith('\uFEFF') ? value.slice(1) : value;
}

function parseCsvColumns(line: string): string[] {
  if (line.trim().length === 0) {
    return [];
  }

  const values: string[] = [];
  let currentValue = '';
  let insideQuotes = false;

  for (let index = 0; index < line.length; index += 1) {
    const currentChar = line[index];

    if (currentChar === '"') {
      if (insideQuotes && line[index + 1] === '"') {
        currentValue += '"';
        index += 1;
      } else {
        insideQuotes = !insideQuotes;
      }

      continue;
    }

    if (currentChar === ',' && insideQuotes === false) {
      values.push(currentValue.trim());
      currentValue = '';
      continue;
    }

    currentValue += currentChar;
  }

  values.push(currentValue.trim());
  return values;
}

async function extractCsvHeadersFromFile(file: File): Promise<string[]> {
  const textContent = await file.text();
  const firstLine = removeUtf8Bom(textContent.split(/\r?\n/)[0] ?? '').trim();
  const headers = parseCsvColumns(firstLine);

  return headers
    .map((header, index) => header.trim() || `column_${index + 1}`)
    .filter((header) => header.length > 0);
}

function normalizeCsvHeaderSelectionOptions(headersByFile: string[][]): string[] {
  if (headersByFile.length === 0) {
    return [];
  }

  const deduplicatedHeadersByFile = headersByFile.map((headers) =>
    Array.from(new Set(headers.filter((header) => header.trim().length > 0))),
  );

  const [firstFileHeaders, ...remainingFileHeaders] = deduplicatedHeadersByFile;
  const commonHeaders = firstFileHeaders.filter((header) =>
    remainingFileHeaders.every((headers) => headers.includes(header)),
  );

  if (commonHeaders.length > 0) {
    return commonHeaders;
  }

  return Array.from(new Set(deduplicatedHeadersByFile.flat()));
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
  datasetFiles: File[];
  onBack: () => void;
  onCompleted: (payload: ConfigureProjectSetupPayload) => void | Promise<void>;
}>;

export function ProjectSetupStep({ datasetFiles, onBack, onCompleted }: ProjectSetupStepProps) {
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
  const [csvHeaderOptions, setCsvHeaderOptions] = useState<string[]>([]);
  const [isLoadingCsvHeaders, setIsLoadingCsvHeaders] = useState(false);
  const [annotationTargetColumn, setAnnotationTargetColumn] = useState('');
  const [useCsvColumnsAsLabels, setUseCsvColumnsAsLabels] = useState(false);

  const datasetItems = useMemo(
    () =>
      datasetFiles.map((file) => ({
        mimeType: file.type || 'application/octet-stream',
        fileName: file.name,
      })),
    [datasetFiles],
  );
  const csvDatasetFiles = useMemo(
    () => datasetFiles.filter((file) => isCsvDatasetFile(file)),
    [datasetFiles],
  );
  const requiresAnnotationTargetColumn = csvDatasetFiles.length > 0;

  const isNerDatasetCompatible = useMemo(
    () => isNerCompatibleDataset(datasetItems),
    [datasetItems],
  );
  const isNerProjectType = projectType === 'NER';
  const requiresLabels = projectType !== 'SEQ2SEQ';
  const isLabelNameValid = draftLabelName.trim().length > 0;
  const isLabelColorValid = !isNerProjectType || draftLabelColor.trim().length > 0;
  const isLabelDialogSaveDisabled = !isLabelNameValid || !isLabelColorValid;
  const canUseCsvColumnsAsLabels =
    requiresLabels &&
    requiresAnnotationTargetColumn &&
    !isLoadingCsvHeaders &&
    csvHeaderOptions.length > 0;
  const selectedLabelNames = useMemo(
    () => new Set(labels.map((label) => label.name.toLowerCase())),
    [labels],
  );
  const editingLabelName =
    editingLabelIndex !== null ? (labels[editingLabelIndex]?.name.trim().toLowerCase() ?? '') : '';
  const availableCsvLabelOptions = useMemo(() => {
    const normalizedTargetColumn = annotationTargetColumn.trim().toLowerCase();

    return csvHeaderOptions
      .filter((header) => {
        const normalizedHeader = header.trim().toLowerCase();

        if (normalizedHeader.length === 0 || normalizedHeader === normalizedTargetColumn) {
          return false;
        }

        return normalizedHeader === editingLabelName || !selectedLabelNames.has(normalizedHeader);
      })
      .map((header) => ({ label: header, value: header }));
  }, [annotationTargetColumn, csvHeaderOptions, editingLabelName, selectedLabelNames]);
  const csvLabelNameOptions =
    useCsvColumnsAsLabels && canUseCsvColumnsAsLabels ? availableCsvLabelOptions : undefined;

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

  useEffect(() => {
    let cancelled = false;

    if (!requiresAnnotationTargetColumn) {
      setCsvHeaderOptions([]);
      setAnnotationTargetColumn('');
      setUseCsvColumnsAsLabels(false);
      setIsLoadingCsvHeaders(false);
      return;
    }

    setIsLoadingCsvHeaders(true);

    void Promise.all(csvDatasetFiles.map((file) => extractCsvHeadersFromFile(file)))
      .then((headersByFile) => {
        if (cancelled) {
          return;
        }

        const normalizedHeaders = normalizeCsvHeaderSelectionOptions(headersByFile);
        setCsvHeaderOptions(normalizedHeaders);

        setAnnotationTargetColumn((currentValue) => {
          const normalizedCurrentValue = currentValue.trim();
          if (
            normalizedCurrentValue.length > 0 &&
            normalizedHeaders.includes(normalizedCurrentValue)
          ) {
            return normalizedCurrentValue;
          }

          return normalizedHeaders[0] ?? '';
        });
      })
      .catch(() => {
        if (cancelled) {
          return;
        }

        setCsvHeaderOptions([]);
        setAnnotationTargetColumn('');
        toast.error(t('project.create.annotationTargetColumnHeadersReadError'));
      })
      .finally(() => {
        if (!cancelled) {
          setIsLoadingCsvHeaders(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [csvDatasetFiles, requiresAnnotationTargetColumn, t]);

  useEffect(() => {
    if (!canUseCsvColumnsAsLabels) {
      setUseCsvColumnsAsLabels(false);
    }
  }, [canUseCsvColumnsAsLabels]);

  useEffect(() => {
    if (!useCsvColumnsAsLabels) {
      return;
    }

    const normalizedTargetColumn = annotationTargetColumn.trim().toLowerCase();
    if (normalizedTargetColumn.length === 0) {
      return;
    }

    setLabels((prev) =>
      prev.filter((label) => label.name.trim().toLowerCase() !== normalizedTargetColumn),
    );
  }, [annotationTargetColumn, useCsvColumnsAsLabels]);

  const hasValidGuideline =
    guidelineMode === 'TEXT' ? guidelineText.trim().length > 0 : guidelinePdfFile !== null;
  const hasValidAnnotationTargetColumn =
    !requiresAnnotationTargetColumn || annotationTargetColumn.trim().length > 0;
  const isClassificationProject =
    projectType === 'TEXT_CLASSIFICATION_SIMPLE' ||
    projectType === 'TEXT_CLASSIFICATION_MULTILABEL';
  const hasMinLabels = isClassificationProject ? labels.length >= 2 : labels.length > 0;

  const canSaveSetup =
    hasValidGuideline &&
    hasValidAnnotationTargetColumn &&
    (!requiresLabels || hasMinLabels) &&
    !isPreparingSetup &&
    !isLoadingCsvHeaders;

  const handleGuidelinePdfSelected = (files: FileList | null) => {
    if (!files || files.length === 0) {
      return;
    }

    const file = files[0];
    if (file.size > MAX_GUIDELINE_SIZE_MB * 1024 * 1024) {
      toast.error(
        t('project.create.fileSizeError', { fileName: file.name, limit: MAX_GUIDELINE_SIZE_MB }),
      );
      return;
    }

    const extension = file.name.split('.').pop()?.toLowerCase();
    if (extension !== 'pdf') {
      toast.error(t('project.create.forbiddenExtensionError', { extension }));
      return;
    }

    setGuidelinePdfFile(file);
    setGuidelineText('');
  };

  const openCreateLabelDialog = () => {
    setLabelDialogMode('create');
    setEditingLabelIndex(null);
    setDraftLabelName('');
    setDraftLabelColor(LABEL_COLOR_PALETTE[0]);
    setIsLabelEditorOpen(true);
  };

  const handleUseCsvColumnsAsLabelsChange = (checked: boolean) => {
    setUseCsvColumnsAsLabels(checked);
    setDraftLabelName('');
    setEditingLabelIndex(null);
    setIsLabelEditorOpen(false);
    setLabels([]);
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

    if (
      useCsvColumnsAsLabels &&
      canUseCsvColumnsAsLabels &&
      !availableCsvLabelOptions.some((option) => option.value === normalizedName)
    ) {
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
      setDraftLabelName('');
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
        annotationTargetColumn:
          requiresAnnotationTargetColumn && annotationTargetColumn.trim().length > 0
            ? annotationTargetColumn.trim()
            : undefined,
      });
    } finally {
      setIsPreparingSetup(false);
    }
  };

  return (
    <div className="mt-8 space-y-6">
      <section className="space-y-3">
        <p className="text-xs font-bold uppercase tracking-wider text-muted-foreground">
          {t('project.create.projectTypeLabel')} *
        </p>

        <ProjectTypeSelector
          disabledTypes={isNerDatasetCompatible === false ? ['NER'] : []}
          onChange={handleProjectTypeChange}
          value={projectType}
        />
      </section>

      {requiresAnnotationTargetColumn && isLoadingCsvHeaders && (
        <p className="inline-flex items-center gap-2 text-sm text-muted-foreground">
          <Spinner aria-hidden className="size-4" />
          {t('project.create.annotationTargetColumnLoading')}
        </p>
      )}

      {requiresAnnotationTargetColumn && !isLoadingCsvHeaders && csvHeaderOptions.length > 0 && (
        <FormFieldControl
          controlType="select"
          id="create-project-annotation-target-column"
          label={t('project.create.annotationTargetColumnLabel')}
          onValueChange={(nextColumn) => {
            setAnnotationTargetColumn(nextColumn);
            setDraftLabelName((currentValue) =>
              currentValue.trim().toLowerCase() === nextColumn.trim().toLowerCase()
                ? ''
                : currentValue,
            );
          }}
          options={[
            ...csvHeaderOptions.map((header) => ({
              label: header,
              value: header,
            })),
          ]}
          required
          selectProps={{ required: true }}
          value={annotationTargetColumn}
        />
      )}

      {requiresAnnotationTargetColumn && !isLoadingCsvHeaders && csvHeaderOptions.length === 0 && (
        <FormFieldControl
          controlType="input"
          id="create-project-annotation-target-column-fallback"
          label={t('project.create.annotationTargetColumnLabel')}
          onValueChange={setAnnotationTargetColumn}
          inputProps={{
            placeholder: t('project.create.annotationTargetColumnPlaceholder'),
            required: true,
          }}
          required
          value={annotationTargetColumn}
        />
      )}

      {requiresLabels && (
        <section>
          <div className="space-y-3">
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

            {canUseCsvColumnsAsLabels && (
              <label
                className="flex cursor-pointer items-start gap-3 rounded-md border border-border bg-white px-4 py-5 text-sm text-primary "
                htmlFor="create-project-use-csv-columns-as-labels"
              >
                <input
                  checked={useCsvColumnsAsLabels}
                  className="mt-0.5 size-4 rounded border-border accent-primary"
                  id="create-project-use-csv-columns-as-labels"
                  onChange={(event) =>
                    handleUseCsvColumnsAsLabelsChange(event.currentTarget.checked)
                  }
                  type="checkbox"
                />
                <span className="block font-semibold">
                  {t('project.create.useCsvColumnsAsLabels')}
                </span>
              </label>
            )}
          </div>

          <div className="mt-4">
            {labels.length === 0 ? (
              <p className="rounded-lg border border-dashed border-border bg-background px-4 py-5 text-sm text-muted-foreground">
                {t('project.create.labelsEmptyState')}
              </p>
            ) : (
              <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
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
          {requiresLabels && isClassificationProject && labels.length > 0 && labels.length < 2 && (
            <p className="mt-2 text-sm font-medium text-destructive">
              {t('project.create.labelsMinCountError')}
            </p>
          )}
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
        placeholder={
          useCsvColumnsAsLabels && canUseCsvColumnsAsLabels
            ? t('project.create.csvLabelNamePlaceholder')
            : t('project.create.labelNamePlaceholder')
        }
        labelNameOptions={csvLabelNameOptions}
        saveCreateText={t('project.create.addLabel')}
        saveEditText={t('project.create.saveLabel')}
        titleCreate={t('project.create.createLabelTitle')}
        titleEdit={t('project.create.editLabelTitle')}
      />
    </div>
  );
}
