import { type ProjectParticipantRole, type ProjectType } from '@/modules/project/types/project';

export function projectTypeI18nKey(type: ProjectType): string {
  const map: Record<ProjectType, string> = {
    TEXT_CLASSIFICATION_SIMPLE: 'project.create.projectTypes.textClassificationSimple',
    TEXT_CLASSIFICATION_MULTILABEL: 'project.create.projectTypes.textClassificationMultiLabel',
    NER: 'project.create.projectTypes.ner',
    SEQ2SEQ: 'project.create.projectTypes.seq2seq',
  };
  return map[type];
}

export function participantRoleI18nKey(role: ProjectParticipantRole): string {
  if (role === 'CREATOR') {
    return 'project.list.roles.creator';
  }

  return 'project.list.roles.participant';
}

export function getPersonInitials(firstName: string, lastName: string): string {
  const firstInitial = firstName.trim().charAt(0).toUpperCase();
  const lastInitial = lastName.trim().charAt(0).toUpperCase();

  return `${firstInitial}${lastInitial}`.trim();
}

export function formatFileSize(bytes: number): string {
  if (!Number.isFinite(bytes) || bytes < 1024) {
    return `${Math.max(0, Math.trunc(bytes))} B`;
  }

  const units = ['KB', 'MB', 'GB', 'TB'];
  let value = bytes / 1024;
  let unitIndex = 0;

  while (value >= 1024 && unitIndex < units.length - 1) {
    value /= 1024;
    unitIndex += 1;
  }

  return `${value.toFixed(1)} ${units[unitIndex]}`;
}

export function isCsvFile(mimeType: string, fileName: string): boolean {
  const normalizedMimeType = mimeType.toLowerCase();
  const normalizedFileName = fileName.toLowerCase();

  return normalizedMimeType.includes('csv') || normalizedFileName.endsWith('.csv');
}
