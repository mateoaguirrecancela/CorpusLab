import { describe, expect, it } from 'vitest';
import {
  getGuidelinePdfRejectionIssue,
  validateGuidelinePdfSelection,
} from '@/modules/project/setup/utils/projectSetupGuidelineUtils';

function file(name: string, sizeBytes: number, type = 'application/pdf'): File {
  return new File([new Uint8Array(sizeBytes)], name, { type });
}

describe('validateGuidelinePdfSelection', () => {
  it('accepts a valid new pdf', () => {
    expect(validateGuidelinePdfSelection(file('guide.pdf', 10), null)).toBeNull();
  });

  it('flags a duplicate of the current file', () => {
    const current = file('guide.pdf', 10);
    expect(validateGuidelinePdfSelection(file('guide.pdf', 10), current)).toEqual({
      type: 'duplicate',
      fileName: 'guide.pdf',
    });
  });

  it('flags a file exceeding the size limit', () => {
    const result = validateGuidelinePdfSelection(file('guide.pdf', 11 * 1024 * 1024), null);
    expect(result).toEqual({ type: 'file-too-large', fileName: 'guide.pdf', limit: 10 });
  });

  it('flags a non-pdf extension', () => {
    const result = validateGuidelinePdfSelection(
      file('guide.docx', 10, 'application/msword'),
      null,
    );
    expect(result).toEqual({ type: 'forbidden-extension', extension: 'docx' });
  });
});

describe('getGuidelinePdfRejectionIssue', () => {
  it('returns null when there are no rejections', () => {
    expect(getGuidelinePdfRejectionIssue([])).toBeNull();
  });

  it('reports a single-file issue for too-many-files', () => {
    const result = getGuidelinePdfRejectionIssue([
      { file: file('a.pdf', 1), errors: [{ code: 'too-many-files' }] },
    ]);
    expect(result).toEqual({ type: 'single-file' });
  });

  it('reports a forbidden-extension issue otherwise', () => {
    const result = getGuidelinePdfRejectionIssue([
      { file: file('a.docx', 1, 'application/msword'), errors: [{ code: 'file-invalid-type' }] },
    ]);
    expect(result).toEqual({ type: 'forbidden-extension', extension: 'docx' });
  });
});
