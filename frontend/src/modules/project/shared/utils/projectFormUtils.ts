import {
  type CreateProjectPayload,
  type UpdateProjectPayload,
} from '@/modules/project/shared/types/project';

export const DIRTY_VALIDATED_FIELD_OPTIONS = {
  shouldDirty: true,
  shouldValidate: true,
} as const;

type ProjectPayloadInput = {
  description?: string | null;
  name: string;
};

export function hasText(value: string): boolean {
  return value.trim().length > 0;
}

export function isPositiveId(value: number): boolean {
  return Number.isFinite(value) && value > 0;
}

export function optionalTrimmedText(value: string | null | undefined): string | undefined {
  const trimmedValue = value?.trim() ?? '';
  return trimmedValue || undefined;
}

export function toProjectPayload(
  values: ProjectPayloadInput,
): CreateProjectPayload & UpdateProjectPayload {
  return {
    name: values.name.trim(),
    description: optionalTrimmedText(values.description),
  };
}
