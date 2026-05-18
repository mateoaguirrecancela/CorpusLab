import { type TFunction } from 'i18next';
import { z } from 'zod';
import {
  type ProjectParticipantAssignmentGroup,
  type ProjectType,
} from '@/modules/project/types/project';
import { isPositiveId } from '@/modules/project/utils/projectFormUtils';

const projectTypeSchema = z.enum([
  'TEXT_CLASSIFICATION_SIMPLE',
  'TEXT_CLASSIFICATION_MULTILABEL',
  'NER',
  'SEQ2SEQ',
] satisfies [ProjectType, ProjectType, ProjectType, ProjectType]);

const assignmentGroupSchema = z.enum(['GROUP_A', 'GROUP_B'] satisfies [
  ProjectParticipantAssignmentGroup,
  ProjectParticipantAssignmentGroup,
]);

function isFileValue(value: unknown): value is File {
  if (value == null || typeof value !== 'object') {
    return false;
  }

  const FileConstructor = globalThis.File;
  if (typeof FileConstructor === 'function') {
    return value instanceof FileConstructor;
  }

  return 'name' in value && 'size' in value && 'type' in value;
}

const labelSchema = z
  .object({
    color: z.string().nullable(),
    name: z.string().trim().min(1).max(128),
  })
  .strict();

export function createProjectInfoSchema(t: TFunction) {
  return z
    .object({
      description: z.string().max(2048, t('project.create.validation.descriptionMax')),
      name: z
        .string()
        .trim()
        .min(1, t('project.create.validation.nameRequired'))
        .max(256, t('project.create.validation.nameMax')),
      selectedGroupId: z
        .string()
        .trim()
        .refine((value) => {
          return isPositiveId(Number(value));
        }, t('project.create.validation.groupRequired')),
    })
    .strict();
}

type ProjectSetupSchemaOptions = Readonly<{
  requiresAnnotationTargetColumn: boolean;
}>;

export function createProjectSetupSchema(
  t: TFunction,
  { requiresAnnotationTargetColumn }: ProjectSetupSchemaOptions,
) {
  return z
    .object({
      annotationTargetColumn: z.string(),
      guidelineMode: z.enum(['TEXT', 'PDF']),
      guidelinePdfFile: z
        .custom<File | null>((value) => value === null || isFileValue(value), {
          message: t('project.create.validation.guidelineRequired'),
        })
        .nullable(),
      guidelineText: z.string().max(5000),
      labels: z.array(labelSchema),
      projectType: projectTypeSchema,
      useCsvColumnsAsLabels: z.boolean(),
    })
    .strict()
    .superRefine((value, context) => {
      if (value.guidelineMode === 'TEXT' && value.guidelineText.trim().length === 0) {
        context.addIssue({
          code: 'custom',
          message: t('project.create.validation.guidelineRequired'),
          path: ['guidelineText'],
        });
      }

      if (value.guidelineMode === 'PDF' && value.guidelinePdfFile === null) {
        context.addIssue({
          code: 'custom',
          message: t('project.create.validation.guidelineRequired'),
          path: ['guidelinePdfFile'],
        });
      }

      if (requiresAnnotationTargetColumn && value.annotationTargetColumn.trim().length === 0) {
        context.addIssue({
          code: 'custom',
          message: t('project.create.validation.annotationTargetColumnRequired'),
          path: ['annotationTargetColumn'],
        });
      }

      if (value.projectType === 'SEQ2SEQ') {
        return;
      }

      const minimumLabels =
        value.projectType === 'TEXT_CLASSIFICATION_SIMPLE' ||
        value.projectType === 'TEXT_CLASSIFICATION_MULTILABEL'
          ? 2
          : 1;

      if (value.labels.length < minimumLabels) {
        context.addIssue({
          code: 'custom',
          message:
            minimumLabels === 2
              ? t('project.create.labelsMinCountError')
              : t('project.create.validation.labelsRequired'),
          path: ['labels'],
        });
      }

      if (
        value.projectType === 'NER' &&
        value.labels.some((label) => label.color == null || label.color.trim().length === 0)
      ) {
        context.addIssue({
          code: 'custom',
          message: t('project.create.validation.labelColorRequired'),
          path: ['labels'],
        });
      }
    });
}

export function createEditProjectSchema(t: TFunction) {
  return z
    .object({
      assignmentByUserId: z.record(z.string(), assignmentGroupSchema),
      description: z.string().max(2048, t('project.create.validation.descriptionMax')),
      name: z
        .string()
        .trim()
        .min(1, t('project.create.validation.nameRequired'))
        .max(256, t('project.create.validation.nameMax')),
    })
    .strict();
}

export type EditProjectFormValues = z.infer<ReturnType<typeof createEditProjectSchema>>;
export type ProjectCreateInfoFormValues = z.infer<ReturnType<typeof createProjectInfoSchema>>;
export type ProjectSetupFormValues = z.infer<ReturnType<typeof createProjectSetupSchema>>;
