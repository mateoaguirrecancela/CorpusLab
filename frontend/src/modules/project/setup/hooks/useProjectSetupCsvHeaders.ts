import { useEffect, useState } from 'react';
import {
  extractCsvHeadersFromFile,
  normalizeCsvHeaderSelectionOptions,
} from '@/modules/project/setup/utils/projectSetupCsvUtils';

type UseProjectSetupCsvHeadersParams = Readonly<{
  csvDatasetFiles: File[];
  disableCsvColumnsAsLabels: () => void;
  getAnnotationTargetColumn: () => string;
  getUseCsvColumnsAsLabels: () => boolean;
  onHeadersReadError: () => void;
  pruneLabelsMatchingTargetColumn: (targetColumn: string) => void;
  requiresAnnotationTargetColumn: boolean;
  setAnnotationTargetColumn: (targetColumn: string) => void;
}>;

type UseProjectSetupCsvHeadersResult = Readonly<{
  csvHeaderOptions: string[];
  isLoadingCsvHeaders: boolean;
}>;

export function useProjectSetupCsvHeaders({
  csvDatasetFiles,
  disableCsvColumnsAsLabels,
  getAnnotationTargetColumn,
  getUseCsvColumnsAsLabels,
  onHeadersReadError,
  pruneLabelsMatchingTargetColumn,
  requiresAnnotationTargetColumn,
  setAnnotationTargetColumn,
}: UseProjectSetupCsvHeadersParams): UseProjectSetupCsvHeadersResult {
  const [csvHeaderOptions, setCsvHeaderOptions] = useState<string[]>([]);
  const [isLoadingCsvHeaders, setIsLoadingCsvHeaders] = useState(false);

  useEffect(() => {
    let cancelled = false;

    if (!requiresAnnotationTargetColumn) {
      setAnnotationTargetColumn('');
      disableCsvColumnsAsLabels();
      return;
    }

    void Promise.resolve().then(() => {
      if (!cancelled) {
        setIsLoadingCsvHeaders(true);
      }
    });

    void Promise.all(csvDatasetFiles.map((file) => extractCsvHeadersFromFile(file)))
      .then((headersByFile) => {
        if (cancelled) {
          return;
        }

        const normalizedHeaders = normalizeCsvHeaderSelectionOptions(headersByFile);
        const currentValue = getAnnotationTargetColumn().trim();
        const nextAnnotationTargetColumn =
          currentValue.length > 0 && normalizedHeaders.includes(currentValue)
            ? currentValue
            : (normalizedHeaders[0] ?? '');

        setCsvHeaderOptions(normalizedHeaders);
        setAnnotationTargetColumn(nextAnnotationTargetColumn);

        if (getUseCsvColumnsAsLabels()) {
          pruneLabelsMatchingTargetColumn(nextAnnotationTargetColumn);
        }
      })
      .catch(() => {
        if (cancelled) {
          return;
        }

        setCsvHeaderOptions([]);
        setAnnotationTargetColumn('');
        onHeadersReadError();
      })
      .finally(() => {
        if (!cancelled) {
          setIsLoadingCsvHeaders(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [
    csvDatasetFiles,
    disableCsvColumnsAsLabels,
    getAnnotationTargetColumn,
    getUseCsvColumnsAsLabels,
    onHeadersReadError,
    pruneLabelsMatchingTargetColumn,
    requiresAnnotationTargetColumn,
    setAnnotationTargetColumn,
  ]);

  return {
    csvHeaderOptions: requiresAnnotationTargetColumn ? csvHeaderOptions : [],
    isLoadingCsvHeaders: requiresAnnotationTargetColumn && isLoadingCsvHeaders,
  };
}
