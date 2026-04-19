import type { DatasetItem } from '@/modules/project/types/project';

const NER_SUPPORTED_EXTENSIONS = new Set(['txt', 'json']);

export function formatDate(value: string): string {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return '-';
  }

  return date.toLocaleDateString(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  });
}

export function normalizeCompletionPercentage(value: number): number {
  if (!Number.isFinite(value)) {
    return 0;
  }

  return Math.min(100, Math.max(0, Math.round(value)));
}

export function completionBadgeClassName(completionPercentage: number): string {
  if (completionPercentage >= 100) {
    return 'bg-emerald-100 text-emerald-800';
  }

  if (completionPercentage >= 50) {
    return 'bg-amber-100 text-amber-800';
  }

  return 'bg-sky-100 text-sky-800';
}

export function completionColor(pct: number): {
  bar: string;
  text: string;
  bg: string;
} {
  if (pct >= 100) {
    return {
      bar: 'bg-emerald-500',
      text: 'text-emerald-700',
      bg: 'bg-emerald-50',
    };
  }

  if (pct >= 50) {
    return {
      bar: 'bg-amber-500',
      text: 'text-amber-700',
      bg: 'bg-amber-50',
    };
  }

  return {
    bar: 'bg-sky-500',
    text: 'text-sky-700',
    bg: 'bg-sky-50',
  };
}

function getFileExtension(fileName: string | null | undefined): string {
  if (!fileName) {
    return '';
  }

  const normalizedName = fileName.trim().toLowerCase();
  const dotIndex = normalizedName.lastIndexOf('.');
  if (dotIndex < 0 || dotIndex === normalizedName.length - 1) {
    return '';
  }

  return normalizedName.slice(dotIndex + 1);
}

export function isNerCompatibleDatasetItem(
  item: Pick<DatasetItem, 'mimeType' | 'fileName'>,
): boolean {
  const mimeType = item.mimeType.trim().toLowerCase();
  const extension = getFileExtension(item.fileName);

  if (
    mimeType === 'text/plain' ||
    mimeType === 'application/json' ||
    mimeType === 'text/json' ||
    mimeType.endsWith('+json')
  ) {
    return true;
  }

  return NER_SUPPORTED_EXTENSIONS.has(extension);
}

export function isNerCompatibleDataset(
  datasetItems: Array<Pick<DatasetItem, 'mimeType' | 'fileName'>>,
): boolean {
  return datasetItems.length > 0 && datasetItems.every((item) => isNerCompatibleDatasetItem(item));
}
