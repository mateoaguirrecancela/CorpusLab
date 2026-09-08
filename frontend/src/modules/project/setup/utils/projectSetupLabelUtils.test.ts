import { describe, expect, it } from 'vitest';
import {
  createLabelCandidate,
  getAvailableCsvLabelOptions,
  hasDuplicateLabelName,
  normalizeLabelsForProjectType,
  removeLabelMatchingName,
} from '@/modules/project/setup/utils/projectSetupLabelUtils';
import { type ProjectSetupLabel } from '@/modules/project/shared/types/project';

describe('normalizeLabelsForProjectType', () => {
  it('clears all labels for SEQ2SEQ', () => {
    expect(normalizeLabelsForProjectType([{ name: 'a', color: null }], 'SEQ2SEQ')).toEqual([]);
  });

  it('assigns a default color to NER labels missing one', () => {
    const labels = normalizeLabelsForProjectType([{ name: 'a', color: null }], 'NER');
    expect(labels[0].color).not.toBeNull();
  });

  it('keeps an existing color for NER labels', () => {
    const labels = normalizeLabelsForProjectType([{ name: 'a', color: '#123456' }], 'NER');
    expect(labels[0].color).toBe('#123456');
  });

  it('strips color for non-NER, non-SEQ2SEQ types', () => {
    const labels = normalizeLabelsForProjectType(
      [{ name: 'a', color: '#123456' }],
      'TEXT_CLASSIFICATION_SIMPLE',
    );
    expect(labels[0].color).toBeNull();
  });
});

describe('hasDuplicateLabelName', () => {
  const labels: ProjectSetupLabel[] = [
    { name: 'Positive', color: null },
    { name: 'Negative', color: null },
  ];

  it('detects a case-insensitive duplicate when creating', () => {
    expect(hasDuplicateLabelName(labels, 'positive', 'create', null)).toBe(true);
  });

  it('ignores the label currently being edited', () => {
    expect(hasDuplicateLabelName(labels, 'positive', 'edit', 0)).toBe(false);
  });

  it('still flags a duplicate against a different label while editing', () => {
    expect(hasDuplicateLabelName(labels, 'negative', 'edit', 0)).toBe(true);
  });
});

describe('createLabelCandidate', () => {
  it('sets a color for NER project types', () => {
    expect(createLabelCandidate('positive', '#ff0000', true)).toEqual({
      color: '#ff0000',
      name: 'positive',
    });
  });

  it('sets a null color for non-NER project types', () => {
    expect(createLabelCandidate('positive', '#ff0000', false)).toEqual({
      color: null,
      name: 'positive',
    });
  });
});

describe('removeLabelMatchingName', () => {
  const labels: ProjectSetupLabel[] = [
    { name: 'Positive', color: null },
    { name: 'Negative', color: null },
  ];

  it('removes the label matching case-insensitively', () => {
    expect(removeLabelMatchingName(labels, 'positive')).toEqual([
      { name: 'Negative', color: null },
    ]);
  });

  it('returns the list unchanged for a blank target name', () => {
    expect(removeLabelMatchingName(labels, '   ')).toEqual(labels);
  });
});

describe('getAvailableCsvLabelOptions', () => {
  it('excludes the target column and already-selected labels', () => {
    const options = getAvailableCsvLabelOptions({
      annotationTargetColumn: 'text',
      csvHeaderOptions: ['text', 'label', 'sentiment'],
      editingLabelName: '',
      labels: [{ name: 'label', color: null }],
    });

    expect(options).toEqual([{ label: 'sentiment', value: 'sentiment' }]);
  });

  it('keeps the header being edited even if it matches a selected label', () => {
    const options = getAvailableCsvLabelOptions({
      annotationTargetColumn: 'text',
      csvHeaderOptions: ['text', 'label'],
      editingLabelName: 'label',
      labels: [{ name: 'label', color: null }],
    });

    expect(options).toEqual([{ label: 'label', value: 'label' }]);
  });
});
