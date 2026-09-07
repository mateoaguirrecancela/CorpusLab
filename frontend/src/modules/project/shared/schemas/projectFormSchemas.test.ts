import { describe, expect, it } from 'vitest';
import { type TFunction } from 'i18next';
import {
  createEditProjectSchema,
  createProjectInfoSchema,
  createProjectSetupSchema,
} from '@/modules/project/shared/schemas/projectFormSchemas';

const t = ((key: string) => key) as TFunction;

describe('createProjectInfoSchema', () => {
  const schema = createProjectInfoSchema(t);

  it('accepts a valid payload', () => {
    expect(
      schema.safeParse({ name: 'Project', description: '', selectedGroupId: '1' }).success,
    ).toBe(true);
  });

  it('rejects a non-positive group id', () => {
    expect(
      schema.safeParse({ name: 'Project', description: '', selectedGroupId: '0' }).success,
    ).toBe(false);
    expect(
      schema.safeParse({ name: 'Project', description: '', selectedGroupId: 'abc' }).success,
    ).toBe(false);
  });

  it('rejects an empty name', () => {
    expect(schema.safeParse({ name: '  ', description: '', selectedGroupId: '1' }).success).toBe(
      false,
    );
  });
});

describe('createProjectSetupSchema', () => {
  const schema = createProjectSetupSchema(t, { requiresAnnotationTargetColumn: false });

  const basePayload = {
    annotationTargetColumn: '',
    guidelineMode: 'TEXT' as const,
    guidelinePdfFile: null,
    guidelineText: 'Follow these instructions',
    labels: [
      { name: 'Positive', color: null },
      { name: 'Negative', color: null },
    ],
    projectType: 'TEXT_CLASSIFICATION_SIMPLE' as const,
    useCsvColumnsAsLabels: false,
  };

  it('accepts a valid classification setup', () => {
    expect(schema.safeParse(basePayload).success).toBe(true);
  });

  it('requires guideline text when guidelineMode is TEXT', () => {
    expect(schema.safeParse({ ...basePayload, guidelineText: '   ' }).success).toBe(false);
  });

  it('requires a guideline pdf file when guidelineMode is PDF', () => {
    expect(
      schema.safeParse({ ...basePayload, guidelineMode: 'PDF', guidelinePdfFile: null }).success,
    ).toBe(false);
  });

  it('requires at least 2 labels for classification project types', () => {
    expect(
      schema.safeParse({ ...basePayload, labels: [{ name: 'Only', color: null }] }).success,
    ).toBe(false);
  });

  it('requires at least 1 label for NER and a color on every label', () => {
    const nerPayload = {
      ...basePayload,
      projectType: 'NER' as const,
      labels: [{ name: 'Person', color: null }],
    };

    expect(schema.safeParse(nerPayload).success).toBe(false);
    expect(
      schema.safeParse({ ...nerPayload, labels: [{ name: 'Person', color: '#ff0000' }] }).success,
    ).toBe(true);
  });

  it('skips the label requirement entirely for SEQ2SEQ', () => {
    expect(
      schema.safeParse({ ...basePayload, projectType: 'SEQ2SEQ' as const, labels: [] }).success,
    ).toBe(true);
  });

  it('requires the annotation target column when the option demands it', () => {
    const strictSchema = createProjectSetupSchema(t, { requiresAnnotationTargetColumn: true });
    expect(strictSchema.safeParse(basePayload).success).toBe(false);
    expect(strictSchema.safeParse({ ...basePayload, annotationTargetColumn: 'text' }).success).toBe(
      true,
    );
  });
});

describe('createEditProjectSchema', () => {
  const schema = createEditProjectSchema(t);

  it('accepts a valid payload with a group assignment map', () => {
    expect(
      schema.safeParse({
        name: 'Project',
        description: '',
        assignmentByUserId: { '1': 'GROUP_A', '2': 'GROUP_B' },
      }).success,
    ).toBe(true);
  });

  it('rejects an invalid assignment group value', () => {
    expect(
      schema.safeParse({
        name: 'Project',
        description: '',
        assignmentByUserId: { '1': 'GROUP_C' },
      }).success,
    ).toBe(false);
  });

  it('rejects an empty name', () => {
    expect(schema.safeParse({ name: '', description: '', assignmentByUserId: {} }).success).toBe(
      false,
    );
  });
});
