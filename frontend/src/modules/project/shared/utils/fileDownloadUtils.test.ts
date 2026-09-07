import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import {
  calculateBase64SizeBytes,
  decodeBase64ToBuffer,
  parseBase64FilePayload,
  triggerBlobDownload,
} from '@/modules/project/shared/utils/fileDownloadUtils';

describe('parseBase64FilePayload', () => {
  it('parses a data URL, extracting mime type and payload', () => {
    expect(parseBase64FilePayload('data:application/pdf;base64,AAAA', 'fallback/type')).toEqual({
      mimeType: 'application/pdf',
      base64Payload: 'AAAA',
    });
  });

  it('falls back to the raw trimmed value and fallback mime type otherwise', () => {
    expect(parseBase64FilePayload('  AAAA  ', 'fallback/type')).toEqual({
      mimeType: 'fallback/type',
      base64Payload: 'AAAA',
    });
  });
});

describe('calculateBase64SizeBytes', () => {
  it('returns the decoded byte length', () => {
    // "AA==" base64-decodes to a single byte
    expect(calculateBase64SizeBytes('AA==')).toBe(1);
  });

  it('returns 0 for invalid base64 input', () => {
    expect(calculateBase64SizeBytes('not valid base64 !!!')).toBe(0);
  });
});

describe('decodeBase64ToBuffer', () => {
  it('decodes into an ArrayBuffer with the expected bytes', () => {
    const buffer = decodeBase64ToBuffer(btoa('hi'));
    expect(new TextDecoder().decode(buffer)).toBe('hi');
  });
});

describe('triggerBlobDownload', () => {
  beforeEach(() => {
    vi.stubGlobal('URL', {
      ...URL,
      createObjectURL: vi.fn(() => 'blob:mock-url'),
      revokeObjectURL: vi.fn(),
    });
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    vi.useRealTimers();
  });

  it('creates a temporary anchor, clicks it, and later revokes the object URL', () => {
    const clickSpy = vi.fn();
    const appendSpy = vi.spyOn(document.body, 'append');
    const createElementSpy = vi.spyOn(document, 'createElement');

    triggerBlobDownload(new Blob(['data']), 'report.pdf');

    const anchor = createElementSpy.mock.results[0]?.value as HTMLAnchorElement;
    expect(anchor.download).toBe('report.pdf');
    expect(anchor.href).toContain('blob:mock-url');
    expect(appendSpy).toHaveBeenCalledWith(anchor);

    expect(URL.revokeObjectURL).not.toHaveBeenCalled();
    vi.advanceTimersByTime(60_000);
    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:mock-url');

    clickSpy.mockRestore();
  });
});
