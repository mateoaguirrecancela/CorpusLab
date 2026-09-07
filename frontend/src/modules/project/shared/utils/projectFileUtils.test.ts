import { describe, expect, it } from 'vitest';
import {
  getFileDuplicateKey,
  getFileExtension,
  isCsvDatasetFile,
  isCsvFile,
} from '@/modules/project/shared/utils/projectFileUtils';

describe('getFileDuplicateKey', () => {
  it('combines a lowercased, trimmed name with the size', () => {
    expect(getFileDuplicateKey({ name: ' Report.CSV ', size: 100 })).toBe('report.csv::100');
  });

  it('treats files with the same name but different sizes as distinct', () => {
    expect(getFileDuplicateKey({ name: 'a.csv', size: 1 })).not.toBe(
      getFileDuplicateKey({ name: 'a.csv', size: 2 }),
    );
  });
});

describe('getFileExtension', () => {
  it('returns the lowercased extension without the dot', () => {
    expect(getFileExtension('Report.CSV')).toBe('csv');
  });

  it('returns empty string for null, undefined, no extension or trailing dot', () => {
    expect(getFileExtension(null)).toBe('');
    expect(getFileExtension(undefined)).toBe('');
    expect(getFileExtension('README')).toBe('');
    expect(getFileExtension('file.')).toBe('');
  });

  it('handles multiple dots by taking the last segment', () => {
    expect(getFileExtension('archive.tar.gz')).toBe('gz');
  });
});

describe('isCsvFile', () => {
  it('is true when the mime type contains csv', () => {
    expect(isCsvFile('data.txt', 'text/csv')).toBe(true);
  });

  it('is true when the extension is csv even with a generic mime type', () => {
    expect(isCsvFile('data.csv', 'application/octet-stream')).toBe(true);
  });

  it('is false otherwise', () => {
    expect(isCsvFile('data.txt', 'text/plain')).toBe(false);
  });

  it('handles null/undefined inputs', () => {
    expect(isCsvFile(null, null)).toBe(false);
  });
});

describe('isCsvDatasetFile', () => {
  it('delegates to isCsvFile using the File name/type', () => {
    const file = new File(['a'], 'data.csv', { type: 'text/csv' });
    expect(isCsvDatasetFile(file)).toBe(true);
  });
});
