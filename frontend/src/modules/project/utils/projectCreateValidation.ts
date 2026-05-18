import { type TFunction } from 'i18next';
import { toast } from 'sonner';
import {
  getFileDuplicateKey,
  getFileExtension,
  isCsvDatasetFile,
} from '@/modules/project/utils/projectFileUtils';

const MAX_FILE_SIZE_MB = 10;
const MAX_TOTAL_SIZE_MB = 50;
const MAX_FILE_SIZE_BYTES = MAX_FILE_SIZE_MB * 1024 * 1024;
const MAX_TOTAL_SIZE_BYTES = MAX_TOTAL_SIZE_MB * 1024 * 1024;

const BANNED_EXTENSIONS = new Set([
  'exe',
  'bat',
  'cmd',
  'sh',
  'php',
  'jsp',
  'asp',
  'aspx',
  'js',
  'vbs',
  'jar',
  'war',
  'ear',
  'bin',
]);

type ValidateProjectFilesParams = Readonly<{
  currentFiles: File[];
  incomingFiles: File[];
  t: TFunction;
}>;

function sumFileSizes(files: readonly File[]): number {
  let totalSize = 0;

  for (const file of files) {
    totalSize += file.size;
  }

  return totalSize;
}

export function validateProjectFiles({
  currentFiles,
  incomingFiles,
  t,
}: ValidateProjectFilesParams): File[] | null {
  for (const file of incomingFiles) {
    if (file.size > MAX_FILE_SIZE_BYTES) {
      toast.error(
        t('project.create.fileSizeError', { fileName: file.name, limit: MAX_FILE_SIZE_MB }),
      );
      return null;
    }

    const extension = getFileExtension(file.name);
    if (extension && BANNED_EXTENSIONS.has(extension)) {
      toast.error(t('project.create.forbiddenExtensionError', { extension }));
      return null;
    }
  }

  const existingFileKeys = new Set(currentFiles.map((file) => getFileDuplicateKey(file)));
  const incomingFileKeys = new Set<string>();

  for (const file of incomingFiles) {
    const duplicateKey = getFileDuplicateKey(file);
    if (existingFileKeys.has(duplicateKey) || incomingFileKeys.has(duplicateKey)) {
      toast.error(t('project.create.duplicateFileError', { fileName: file.name }));
      return null;
    }

    incomingFileKeys.add(duplicateKey);
  }

  const candidateFiles = [...currentFiles, ...incomingFiles];
  const totalSize = sumFileSizes(candidateFiles);

  if (totalSize > MAX_TOTAL_SIZE_BYTES) {
    toast.error(t('project.create.totalSizeError', { limit: MAX_TOTAL_SIZE_MB }));
    return null;
  }

  const containsCsv = candidateFiles.some(isCsvDatasetFile);

  if (containsCsv && candidateFiles.length > 1) {
    toast.error(t('project.create.csvSingleFileError'));
    return null;
  }

  return candidateFiles;
}
