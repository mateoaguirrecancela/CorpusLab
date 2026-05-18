import { useCallback, useMemo, useState } from 'react';
import { zodResolver } from '@hookform/resolvers/zod';
import { type FieldErrors, useForm, useWatch } from 'react-hook-form';
import { useTranslation } from 'react-i18next';
import { toast } from 'sonner';
import { type UploadDropzoneFileRejection } from '@/modules/project/components/UploadDropzone';
import { useProjectSetupCsvHeaders } from '@/modules/project/hooks/useProjectSetupCsvHeaders';
import { useProjectSetupLabelEditor } from '@/modules/project/hooks/useProjectSetupLabelEditor';
import {
  createProjectSetupSchema,
  type ProjectSetupFormValues,
} from '@/modules/project/schemas/projectFormSchemas';
import {
  type ConfigureProjectSetupPayload,
  type ProjectSetupLabel,
  type ProjectType,
} from '@/modules/project/types/project';
import {
  DIRTY_VALIDATED_FIELD_OPTIONS,
  optionalTrimmedText,
} from '@/modules/project/utils/projectFormUtils';
import { isCsvDatasetFile } from '@/modules/project/utils/projectSetupCsvUtils';
import {
  getGuidelinePdfRejectionIssue,
  validateGuidelinePdfSelection,
  type GuidelinePdfSelectionIssue,
} from '@/modules/project/utils/projectSetupGuidelineUtils';
import {
  normalizeLabelsForProjectType,
  removeLabelMatchingName,
  type SetProjectSetupLabels,
} from '@/modules/project/utils/projectSetupLabelUtils';
import { isNerCompatibleDataset } from '@/modules/project/utils/projectUtils';

type UseProjectSetupStepParams = Readonly<{
  datasetFiles: File[];
  onCompleted: (payload: ConfigureProjectSetupPayload) => void | Promise<void>;
}>;

type UseProjectSetupStepResult = Readonly<{
  annotationTargetColumn: string;
  canSaveSetup: boolean;
  canUseCsvColumnsAsLabels: boolean;
  csvHeaderOptions: string[];
  guidelineMode: ProjectSetupFormValues['guidelineMode'];
  guidelinePdfFile: File | null;
  guidelineText: string;
  handleAnnotationTargetColumnChange: (nextColumn: string) => void;
  handleGuidelineModeChange: (nextMode: ProjectSetupFormValues['guidelineMode']) => void;
  handleGuidelinePdfRejected: (fileRejections: UploadDropzoneFileRejection[]) => void;
  handleGuidelinePdfRemove: () => void;
  handleGuidelinePdfSelected: (files: File[]) => void;
  handleGuidelineTextChange: (nextText: string) => void;
  handleProjectTypeChange: (nextType: ProjectType) => void;
  handleSaveProjectSetup: () => Promise<void>;
  handleUseCsvColumnsAsLabelsChange: (checked: boolean) => void;
  isCsvLabelModeEnabled: boolean;
  isLoadingCsvHeaders: boolean;
  isNerDatasetCompatible: boolean;
  isNerProjectType: boolean;
  isPreparingSetup: boolean;
  labelEditor: ReturnType<typeof useProjectSetupLabelEditor>;
  labels: ProjectSetupLabel[];
  projectType: ProjectType;
  requiresAnnotationTargetColumn: boolean;
  requiresLabels: boolean;
  setupErrors: FieldErrors<ProjectSetupFormValues>;
  shouldShowClassificationLabelMinError: boolean;
}>;

export function useProjectSetupStep({
  datasetFiles,
  onCompleted,
}: UseProjectSetupStepParams): UseProjectSetupStepResult {
  const { t } = useTranslation();
  const csvDatasetFiles = useMemo(
    () => datasetFiles.filter((file) => isCsvDatasetFile(file)),
    [datasetFiles],
  );
  const requiresAnnotationTargetColumn = csvDatasetFiles.length > 0;
  const projectSetupSchema = useMemo(
    () => createProjectSetupSchema(t, { requiresAnnotationTargetColumn }),
    [requiresAnnotationTargetColumn, t],
  );
  const setupForm = useForm<ProjectSetupFormValues>({
    defaultValues: {
      annotationTargetColumn: '',
      guidelineMode: 'TEXT',
      guidelinePdfFile: null,
      guidelineText: '',
      labels: [],
      projectType: 'TEXT_CLASSIFICATION_SIMPLE',
      useCsvColumnsAsLabels: false,
    },
    mode: 'onChange',
    resolver: zodResolver(projectSetupSchema),
  });
  const {
    formState: { errors: setupErrors },
    control,
    getValues,
    setValue,
    trigger,
  } = setupForm;
  const projectType = useWatch({ control, name: 'projectType' });
  const labels = useWatch({ control, name: 'labels' });
  const guidelineMode = useWatch({ control, name: 'guidelineMode' });
  const guidelineText = useWatch({ control, name: 'guidelineText' });
  const guidelinePdfFile = useWatch({ control, name: 'guidelinePdfFile' });
  const annotationTargetColumn = useWatch({ control, name: 'annotationTargetColumn' });
  const useCsvColumnsAsLabels = useWatch({ control, name: 'useCsvColumnsAsLabels' });
  const [isPreparingSetup, setIsPreparingSetup] = useState(false);

  const datasetItems = useMemo(
    () =>
      datasetFiles.map((file) => ({
        mimeType: file.type || 'application/octet-stream',
        fileName: file.name,
      })),
    [datasetFiles],
  );
  const isNerDatasetCompatible = useMemo(
    () => isNerCompatibleDataset(datasetItems),
    [datasetItems],
  );
  const isNerProjectType = projectType === 'NER';
  const requiresLabels = projectType !== 'SEQ2SEQ';
  const setLabelsValue = useCallback<SetProjectSetupLabels>(
    (nextLabels) => {
      const resolvedLabels =
        typeof nextLabels === 'function' ? nextLabels(getValues('labels')) : nextLabels;
      setValue('labels', resolvedLabels, DIRTY_VALIDATED_FIELD_OPTIONS);
    },
    [getValues, setValue],
  );
  const setAnnotationTargetColumn = useCallback(
    (targetColumn: string) => {
      setValue('annotationTargetColumn', targetColumn, DIRTY_VALIDATED_FIELD_OPTIONS);
    },
    [setValue],
  );
  const disableCsvColumnsAsLabels = useCallback(() => {
    setValue('useCsvColumnsAsLabels', false, DIRTY_VALIDATED_FIELD_OPTIONS);
  }, [setValue]);
  const pruneLabelsMatchingTargetColumn = useCallback(
    (targetColumn: string) => {
      setLabelsValue((previousLabels) => removeLabelMatchingName(previousLabels, targetColumn));
    },
    [setLabelsValue],
  );
  const handleCsvHeadersReadError = useCallback(() => {
    toast.error(t('project.create.annotationTargetColumnHeadersReadError'));
  }, [t]);
  const getAnnotationTargetColumn = useCallback(
    () => getValues('annotationTargetColumn'),
    [getValues],
  );
  const getUseCsvColumnsAsLabels = useCallback(
    () => getValues('useCsvColumnsAsLabels'),
    [getValues],
  );
  const { csvHeaderOptions, isLoadingCsvHeaders } = useProjectSetupCsvHeaders({
    csvDatasetFiles,
    disableCsvColumnsAsLabels,
    getAnnotationTargetColumn,
    getUseCsvColumnsAsLabels,
    onHeadersReadError: handleCsvHeadersReadError,
    pruneLabelsMatchingTargetColumn,
    requiresAnnotationTargetColumn,
    setAnnotationTargetColumn,
  });
  const canUseCsvColumnsAsLabels =
    requiresLabels &&
    requiresAnnotationTargetColumn &&
    !isLoadingCsvHeaders &&
    csvHeaderOptions.length > 0;
  const isCsvLabelModeEnabled = useCsvColumnsAsLabels && canUseCsvColumnsAsLabels;
  const labelEditor = useProjectSetupLabelEditor({
    annotationTargetColumn,
    csvHeaderOptions,
    isCsvLabelModeEnabled,
    isNerProjectType,
    labels,
    onDuplicateLabel: () => toast.error(t('project.create.labelsDuplicateError')),
    setLabelsValue,
  });

  const applyProjectType = useCallback(
    (nextType: ProjectType) => {
      setValue('projectType', nextType, DIRTY_VALIDATED_FIELD_OPTIONS);
      setLabelsValue((previousLabels) => normalizeLabelsForProjectType(previousLabels, nextType));
      if (nextType === 'SEQ2SEQ') {
        setValue('useCsvColumnsAsLabels', false, DIRTY_VALIDATED_FIELD_OPTIONS);
      }
    },
    [setLabelsValue, setValue],
  );

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

  const hasValidGuideline =
    guidelineMode === 'TEXT' ? guidelineText.trim().length > 0 : guidelinePdfFile !== null;
  const hasValidAnnotationTargetColumn =
    !requiresAnnotationTargetColumn || annotationTargetColumn.trim().length > 0;
  const isClassificationProject =
    projectType === 'TEXT_CLASSIFICATION_SIMPLE' ||
    projectType === 'TEXT_CLASSIFICATION_MULTILABEL';
  const hasMinLabels = isClassificationProject ? labels.length >= 2 : labels.length > 0;
  const shouldShowClassificationLabelMinError =
    requiresLabels && isClassificationProject && labels.length > 0 && labels.length < 2;
  const canSaveSetup =
    hasValidGuideline &&
    hasValidAnnotationTargetColumn &&
    (!requiresLabels || hasMinLabels) &&
    !isPreparingSetup &&
    !isLoadingCsvHeaders;

  const showGuidelinePdfIssue = useCallback(
    (issue: GuidelinePdfSelectionIssue | { type: 'single-file' }) => {
      if (issue.type === 'duplicate') {
        toast.error(t('project.create.duplicateFileError', { fileName: issue.fileName }));
        return;
      }

      if (issue.type === 'file-too-large') {
        toast.error(
          t('project.create.fileSizeError', { fileName: issue.fileName, limit: issue.limit }),
        );
        return;
      }

      if (issue.type === 'single-file') {
        toast.error(t('project.create.singleFileError'));
        return;
      }

      toast.error(t('project.create.forbiddenExtensionError', { extension: issue.extension }));
    },
    [t],
  );

  const handleAnnotationTargetColumnChange = useCallback(
    (nextColumn: string) => {
      setAnnotationTargetColumn(nextColumn);
      labelEditor.setDraftLabelName((currentValue) =>
        currentValue.trim().toLowerCase() === nextColumn.trim().toLowerCase() ? '' : currentValue,
      );

      if (isCsvLabelModeEnabled) {
        setLabelsValue((previousLabels) => removeLabelMatchingName(previousLabels, nextColumn));
      }
    },
    [isCsvLabelModeEnabled, labelEditor, setAnnotationTargetColumn, setLabelsValue],
  );

  const handleGuidelineModeChange = useCallback(
    (nextMode: ProjectSetupFormValues['guidelineMode']) => {
      setValue('guidelineMode', nextMode, DIRTY_VALIDATED_FIELD_OPTIONS);

      if (nextMode === 'TEXT') {
        setValue('guidelinePdfFile', null, DIRTY_VALIDATED_FIELD_OPTIONS);
        return;
      }

      setValue('guidelineText', '', DIRTY_VALIDATED_FIELD_OPTIONS);
    },
    [setValue],
  );

  const handleGuidelineTextChange = useCallback(
    (nextText: string) => {
      setValue('guidelineText', nextText, DIRTY_VALIDATED_FIELD_OPTIONS);
    },
    [setValue],
  );

  const handleGuidelinePdfSelected = useCallback(
    (files: File[]) => {
      if (files.length === 0) {
        return;
      }

      const file = files[0];
      const validationIssue = validateGuidelinePdfSelection(file, guidelinePdfFile);
      if (validationIssue) {
        showGuidelinePdfIssue(validationIssue);
        return;
      }

      setValue('guidelinePdfFile', file, DIRTY_VALIDATED_FIELD_OPTIONS);
      setValue('guidelineText', '', DIRTY_VALIDATED_FIELD_OPTIONS);
    },
    [guidelinePdfFile, setValue, showGuidelinePdfIssue],
  );

  const handleGuidelinePdfRejected = useCallback(
    (fileRejections: UploadDropzoneFileRejection[]) => {
      const validationIssue = getGuidelinePdfRejectionIssue(fileRejections);
      if (!validationIssue) {
        return;
      }

      showGuidelinePdfIssue(validationIssue);
    },
    [showGuidelinePdfIssue],
  );

  const handleGuidelinePdfRemove = useCallback(() => {
    setValue('guidelinePdfFile', null, DIRTY_VALIDATED_FIELD_OPTIONS);
  }, [setValue]);

  const handleUseCsvColumnsAsLabelsChange = useCallback(
    (checked: boolean) => {
      setValue('useCsvColumnsAsLabels', checked, DIRTY_VALIDATED_FIELD_OPTIONS);
      labelEditor.resetLabelEditor();
      setLabelsValue([]);
    },
    [labelEditor, setLabelsValue, setValue],
  );

  const handleSaveProjectSetup = useCallback(async () => {
    if (!canSaveSetup || isPreparingSetup) {
      return;
    }

    const isSetupValid = await trigger();
    if (!isSetupValid) {
      return;
    }

    const formValues = projectSetupSchema.parse(getValues());
    setIsPreparingSetup(true);

    try {
      await onCompleted({
        projectType: formValues.projectType,
        labels: formValues.labels,
        guidelineText:
          formValues.guidelineMode === 'TEXT'
            ? optionalTrimmedText(formValues.guidelineText)
            : undefined,
        guidelinePdfFile:
          formValues.guidelineMode === 'PDF'
            ? (formValues.guidelinePdfFile ?? undefined)
            : undefined,
        annotationTargetColumn: requiresAnnotationTargetColumn
          ? optionalTrimmedText(formValues.annotationTargetColumn)
          : undefined,
      });
    } finally {
      setIsPreparingSetup(false);
    }
  }, [
    canSaveSetup,
    getValues,
    isPreparingSetup,
    onCompleted,
    projectSetupSchema,
    requiresAnnotationTargetColumn,
    trigger,
  ]);

  return {
    annotationTargetColumn,
    canSaveSetup,
    canUseCsvColumnsAsLabels,
    csvHeaderOptions,
    guidelineMode,
    guidelinePdfFile,
    guidelineText,
    handleAnnotationTargetColumnChange,
    handleGuidelineModeChange,
    handleGuidelinePdfRejected,
    handleGuidelinePdfRemove,
    handleGuidelinePdfSelected,
    handleGuidelineTextChange,
    handleProjectTypeChange,
    handleSaveProjectSetup,
    handleUseCsvColumnsAsLabelsChange,
    isCsvLabelModeEnabled,
    isLoadingCsvHeaders,
    isNerDatasetCompatible,
    isNerProjectType,
    isPreparingSetup,
    labelEditor,
    labels,
    projectType,
    requiresAnnotationTargetColumn,
    requiresLabels,
    setupErrors,
    shouldShowClassificationLabelMinError,
  };
}
