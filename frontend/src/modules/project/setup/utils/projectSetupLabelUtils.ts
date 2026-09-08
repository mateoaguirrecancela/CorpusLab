import { LABEL_COLOR_PALETTE } from '@/modules/project/setup/constants/labelColorPalette';
import { type ProjectSetupLabel, type ProjectType } from '@/modules/project/shared/types/project';

export type LabelDialogMode = 'create' | 'edit';
export type LabelNameOption = { label: string; value: string };
export type ProjectSetupLabelsUpdate =
  | ProjectSetupLabel[]
  | ((currentLabels: ProjectSetupLabel[]) => ProjectSetupLabel[]);
export type SetProjectSetupLabels = (nextLabels: ProjectSetupLabelsUpdate) => void;

function normalizeLabelName(labelName: string): string {
  return labelName.trim().toLowerCase();
}

export function normalizeLabelsForProjectType(
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

export function hasDuplicateLabelName(
  labels: ProjectSetupLabel[],
  normalizedName: string,
  mode: LabelDialogMode,
  editingLabelIndex: number | null,
): boolean {
  return labels.some((label, index) => {
    if (mode === 'edit' && editingLabelIndex === index) {
      return false;
    }

    return normalizeLabelName(label.name) === normalizeLabelName(normalizedName);
  });
}

export function createLabelCandidate(
  normalizedName: string,
  draftLabelColor: string,
  isNerProjectType: boolean,
): ProjectSetupLabel {
  return {
    color: isNerProjectType ? draftLabelColor : null,
    name: normalizedName,
  };
}

export function removeLabelMatchingName(
  labels: ProjectSetupLabel[],
  targetName: string,
): ProjectSetupLabel[] {
  const normalizedTargetName = normalizeLabelName(targetName);
  if (normalizedTargetName.length === 0) {
    return labels;
  }

  return labels.filter((label) => normalizeLabelName(label.name) !== normalizedTargetName);
}

type GetAvailableCsvLabelOptionsParams = Readonly<{
  annotationTargetColumn: string;
  csvHeaderOptions: string[];
  editingLabelName: string;
  labels: ProjectSetupLabel[];
}>;

export function getAvailableCsvLabelOptions({
  annotationTargetColumn,
  csvHeaderOptions,
  editingLabelName,
  labels,
}: GetAvailableCsvLabelOptionsParams): LabelNameOption[] {
  const normalizedTargetColumn = normalizeLabelName(annotationTargetColumn);
  const normalizedEditingLabelName = normalizeLabelName(editingLabelName);
  const selectedLabelNames = new Set(labels.map((label) => normalizeLabelName(label.name)));

  return csvHeaderOptions
    .filter((header) => {
      const normalizedHeader = normalizeLabelName(header);

      if (normalizedHeader.length === 0 || normalizedHeader === normalizedTargetColumn) {
        return false;
      }

      return (
        normalizedHeader === normalizedEditingLabelName || !selectedLabelNames.has(normalizedHeader)
      );
    })
    .map((header) => ({ label: header, value: header }));
}
