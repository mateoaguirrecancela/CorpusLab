import { type TFunction } from 'i18next';
import { toast } from 'sonner';

const MAX_FILE_SIZE_MB = 10;
const MAX_TOTAL_SIZE_MB = 50;

const BANNED_EXTENSIONS = [
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
];

function getFileDuplicateKey(file: File): string {
  return `${file.name.trim().toLowerCase()}::${file.size}`;
}

type ValidateProjectFilesParams = Readonly<{
  currentFiles: File[];
  incomingFiles: File[];
  t: TFunction;
}>;

export function validateProjectFiles({
  currentFiles,
  incomingFiles,
  t,
}: ValidateProjectFilesParams): File[] | null {
  for (const file of incomingFiles) {
    if (file.size > MAX_FILE_SIZE_MB * 1024 * 1024) {
      toast.error(
        t('project.create.fileSizeError', { fileName: file.name, limit: MAX_FILE_SIZE_MB }),
      );
      return null;
    }

    const extension = file.name.split('.').pop()?.toLowerCase();
    if (extension && BANNED_EXTENSIONS.includes(extension)) {
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
  const totalSize = candidateFiles.reduce((sum, file) => sum + file.size, 0);

  if (totalSize > MAX_TOTAL_SIZE_MB * 1024 * 1024) {
    toast.error(t('project.create.totalSizeError', { limit: MAX_TOTAL_SIZE_MB }));
    return null;
  }

  const containsCsv = candidateFiles.some(
    (file) => file.name.toLowerCase().endsWith('.csv') || file.type.toLowerCase().includes('csv'),
  );

  if (containsCsv && candidateFiles.length > 1) {
    toast.error(t('project.create.csvSingleFileError'));
    return null;
  }

  return candidateFiles;
}
