import { describe, expect, it } from 'vitest';
import {
  extractCsvHeadersFromFile,
  normalizeCsvHeaderSelectionOptions,
} from '@/modules/project/setup/utils/projectSetupCsvUtils';

function csvFile(content: string): File {
  return new File([content], 'data.csv', { type: 'text/csv' });
}

describe('extractCsvHeadersFromFile', () => {
  it('extracts and trims the header row', async () => {
    const headers = await extractCsvHeadersFromFile(csvFile('text, label\nhello,positive\n'));
    expect(headers).toEqual(['text', 'label']);
  });

  it('fills in a placeholder name for blank header cells', async () => {
    const headers = await extractCsvHeadersFromFile(csvFile('text,,label\n'));
    expect(headers).toEqual(['text', 'column_2', 'label']);
  });

  it('rejects an empty file since the parser cannot detect a delimiter', async () => {
    await expect(extractCsvHeadersFromFile(csvFile(''))).rejects.toThrow();
  });

  it('throws with the parser message when the csv is malformed', async () => {
    await expect(extractCsvHeadersFromFile(csvFile('"unterminated'))).rejects.toThrow(
      /quote/i,
    );
  });
});

describe('normalizeCsvHeaderSelectionOptions', () => {
  it('returns an empty array with no files', () => {
    expect(normalizeCsvHeaderSelectionOptions([])).toEqual([]);
  });

  it('returns headers common to every file when there is overlap', () => {
    expect(
      normalizeCsvHeaderSelectionOptions([
        ['text', 'label'],
        ['text', 'other'],
      ]),
    ).toEqual(['text']);
  });

  it('falls back to the union of all headers when there is no overlap', () => {
    expect(normalizeCsvHeaderSelectionOptions([['a', 'b'], ['c']])).toEqual(['a', 'b', 'c']);
  });

  it('dedupes headers within a single file', () => {
    expect(normalizeCsvHeaderSelectionOptions([['a', 'a', 'b']])).toEqual(['a', 'b']);
  });
});
