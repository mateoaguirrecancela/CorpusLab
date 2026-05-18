import { useMemo, useState } from 'react';
import { LABEL_COLOR_PALETTE } from '@/modules/project/constants/labelColorPalette';
import { type ProjectSetupLabel } from '@/modules/project/types/project';
import {
  createLabelCandidate,
  getAvailableCsvLabelOptions,
  hasDuplicateLabelName,
  type LabelDialogMode,
  type LabelNameOption,
  type SetProjectSetupLabels,
} from '@/modules/project/utils/projectSetupLabelUtils';

type UseProjectSetupLabelEditorParams = Readonly<{
  annotationTargetColumn: string;
  csvHeaderOptions: string[];
  isCsvLabelModeEnabled: boolean;
  isNerProjectType: boolean;
  labels: ProjectSetupLabel[];
  onDuplicateLabel: () => void;
  setLabelsValue: SetProjectSetupLabels;
}>;

type UseProjectSetupLabelEditorResult = Readonly<{
  availableCsvLabelOptions: LabelNameOption[];
  csvLabelNameOptions: LabelNameOption[] | undefined;
  draftLabelColor: string;
  draftLabelName: string;
  isLabelDialogSaveDisabled: boolean;
  isLabelEditorOpen: boolean;
  labelDialogMode: LabelDialogMode;
  openCreateLabelDialog: () => void;
  openEditLabelDialog: (index: number) => void;
  removeLabelAt: (index: number) => void;
  resetLabelEditor: () => void;
  saveLabelFromDialog: () => void;
  setDraftLabelColor: (nextColor: string) => void;
  setDraftLabelName: (nextName: string | ((currentValue: string) => string)) => void;
  setIsLabelEditorOpen: (isOpen: boolean) => void;
}>;

export function useProjectSetupLabelEditor({
  annotationTargetColumn,
  csvHeaderOptions,
  isCsvLabelModeEnabled,
  isNerProjectType,
  labels,
  onDuplicateLabel,
  setLabelsValue,
}: UseProjectSetupLabelEditorParams): UseProjectSetupLabelEditorResult {
  const [isLabelEditorOpen, setIsLabelEditorOpen] = useState(false);
  const [labelDialogMode, setLabelDialogMode] = useState<LabelDialogMode>('create');
  const [editingLabelIndex, setEditingLabelIndex] = useState<number | null>(null);
  const [draftLabelName, setDraftLabelName] = useState('');
  const [draftLabelColor, setDraftLabelColor] = useState(LABEL_COLOR_PALETTE[0]);
  const editingLabelName =
    editingLabelIndex !== null ? (labels[editingLabelIndex]?.name.trim().toLowerCase() ?? '') : '';
  const availableCsvLabelOptions = useMemo(
    () =>
      getAvailableCsvLabelOptions({
        annotationTargetColumn,
        csvHeaderOptions,
        editingLabelName,
        labels,
      }),
    [annotationTargetColumn, csvHeaderOptions, editingLabelName, labels],
  );
  const isLabelNameValid = draftLabelName.trim().length > 0;
  const isLabelColorValid = !isNerProjectType || draftLabelColor.trim().length > 0;
  const csvLabelNameOptions = isCsvLabelModeEnabled ? availableCsvLabelOptions : undefined;

  const resetLabelEditor = () => {
    setDraftLabelName('');
    setEditingLabelIndex(null);
    setIsLabelEditorOpen(false);
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
    setLabelsValue((prev) => prev.filter((_, idx) => idx !== index));
  };

  const saveLabelFromDialog = () => {
    const normalizedName = draftLabelName.trim();
    if (normalizedName.length === 0) {
      return;
    }

    if (
      isCsvLabelModeEnabled &&
      !availableCsvLabelOptions.some((option) => option.value === normalizedName)
    ) {
      return;
    }

    if (hasDuplicateLabelName(labels, normalizedName, labelDialogMode, editingLabelIndex)) {
      onDuplicateLabel();
      return;
    }

    const candidate = createLabelCandidate(normalizedName, draftLabelColor, isNerProjectType);

    if (labelDialogMode === 'create') {
      setLabelsValue((prev) => [...prev, candidate]);
      setDraftLabelName('');
    } else if (editingLabelIndex !== null) {
      setLabelsValue((prev) =>
        prev.map((label, index) => (index === editingLabelIndex ? candidate : label)),
      );
    }

    setIsLabelEditorOpen(false);
  };

  return {
    availableCsvLabelOptions,
    csvLabelNameOptions,
    draftLabelColor,
    draftLabelName,
    isLabelDialogSaveDisabled: !isLabelNameValid || !isLabelColorValid,
    isLabelEditorOpen,
    labelDialogMode,
    openCreateLabelDialog,
    openEditLabelDialog,
    removeLabelAt,
    resetLabelEditor,
    saveLabelFromDialog,
    setDraftLabelColor,
    setDraftLabelName,
    setIsLabelEditorOpen,
  };
}
