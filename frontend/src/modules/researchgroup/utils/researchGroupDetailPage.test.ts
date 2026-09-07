import { describe, expect, it } from 'vitest';
import {
  getAssignedProjectsFromPages,
  isValidResearchGroupId,
} from '@/modules/researchgroup/utils/researchGroupDetailPage';
import { type ResearchGroupAssignedProjectSummary } from '@/modules/researchgroup/types/researchGroup';

describe('isValidResearchGroupId', () => {
  it('accepts positive finite integers', () => {
    expect(isValidResearchGroupId(1)).toBe(true);
  });

  it('rejects zero, negative and non-finite values', () => {
    expect(isValidResearchGroupId(0)).toBe(false);
    expect(isValidResearchGroupId(-1)).toBe(false);
    expect(isValidResearchGroupId(Number.NaN)).toBe(false);
    expect(isValidResearchGroupId(Number.POSITIVE_INFINITY)).toBe(false);
  });
});

describe('getAssignedProjectsFromPages', () => {
  it('returns an empty array when data is undefined', () => {
    expect(getAssignedProjectsFromPages(undefined)).toEqual([]);
  });

  it('flattens the content of every page', () => {
    const project = { id: 1 } as unknown as ResearchGroupAssignedProjectSummary;
    const otherProject = { id: 2 } as unknown as ResearchGroupAssignedProjectSummary;

    const result = getAssignedProjectsFromPages({
      pages: [
        { content: [project], number: 0, size: 1, totalElements: 2, totalPages: 2 } as never,
        { content: [otherProject], number: 1, size: 1, totalElements: 2, totalPages: 2 } as never,
      ],
      pageParams: [],
    });

    expect(result).toEqual([project, otherProject]);
  });
});
