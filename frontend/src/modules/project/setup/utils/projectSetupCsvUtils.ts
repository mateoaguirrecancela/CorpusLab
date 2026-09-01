import * as Papa from 'papaparse';

export { isCsvDatasetFile } from '@/modules/project/shared/utils/projectFileUtils';

export async function extractCsvHeadersFromFile(file: File): Promise<string[]> {
  const textContent = await file.text();
  const parseResult = Papa.parse<string[]>(textContent, {
    preview: 1,
    skipEmptyLines: 'greedy',
  });

  if (parseResult.errors.length > 0) {
    throw new Error(parseResult.errors[0]?.message ?? 'Could not parse CSV headers');
  }

  const headers = parseResult.data[0] ?? [];

  return headers
    .map((header, index) => header.trim() || `column_${index + 1}`)
    .filter((header) => header.length > 0);
}

export function normalizeCsvHeaderSelectionOptions(headersByFile: string[][]): string[] {
  if (headersByFile.length === 0) {
    return [];
  }

  const deduplicatedHeadersByFile = headersByFile.map((headers) =>
    Array.from(new Set(headers.filter((header) => header.trim().length > 0))),
  );

  const [firstFileHeaders, ...remainingFileHeaders] = deduplicatedHeadersByFile;
  const commonHeaders = firstFileHeaders.filter((header) =>
    remainingFileHeaders.every((headers) => headers.includes(header)),
  );

  if (commonHeaders.length > 0) {
    return commonHeaders;
  }

  return Array.from(new Set(deduplicatedHeadersByFile.flat()));
}
