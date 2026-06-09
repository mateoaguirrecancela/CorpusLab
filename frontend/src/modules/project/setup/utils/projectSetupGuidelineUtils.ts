import {
  getFileDuplicateKey,
  getFileExtension,
} from '@/modules/project/shared/utils/projectFileUtils';

export const MAX_GUIDELINE_SIZE_MB = 10;
export const GUIDELINE_PDF_ACCEPT: Record<string, string[]> = {
  'application/pdf': ['.pdf'],
};

export type GuidelinePdfSelectionIssue =
  | { type: 'duplicate'; fileName: string }
  | { type: 'file-too-large'; fileName: string; limit: number }
  | { type: 'forbidden-extension'; extension: string | undefined };

type DropzoneFileRejectionLike = Readonly<{
  errors: ReadonlyArray<{ code: string }>;
  file: File;
}>;

export function validateGuidelinePdfSelection(
  file: File,
  currentFile: File | null,
): GuidelinePdfSelectionIssue | null {
  if (currentFile && getFileDuplicateKey(file) === getFileDuplicateKey(currentFile)) {
    return { fileName: file.name, type: 'duplicate' };
  }

  if (file.size > MAX_GUIDELINE_SIZE_MB * 1024 * 1024) {
    return { fileName: file.name, limit: MAX_GUIDELINE_SIZE_MB, type: 'file-too-large' };
  }

  const extension = getFileExtension(file.name);
  if (extension !== 'pdf') {
    return { extension, type: 'forbidden-extension' };
  }

  return null;
}

export function getGuidelinePdfRejectionIssue(
  fileRejections: DropzoneFileRejectionLike[],
): GuidelinePdfSelectionIssue | { type: 'single-file' } | null {
  const firstRejection = fileRejections[0];
  if (!firstRejection) {
    return null;
  }

  if (firstRejection.errors.some((error) => error.code === 'too-many-files')) {
    return { type: 'single-file' };
  }

  return {
    extension: getFileExtension(firstRejection.file.name),
    type: 'forbidden-extension',
  };
}
