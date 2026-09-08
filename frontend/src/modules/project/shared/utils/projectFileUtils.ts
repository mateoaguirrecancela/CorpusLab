export function getFileDuplicateKey(file: Pick<File, 'name' | 'size'>): string {
  return `${file.name.trim().toLowerCase()}::${file.size}`;
}

export function getFileExtension(fileName: string | null | undefined): string {
  if (!fileName) {
    return '';
  }

  const normalizedName = fileName.trim().toLowerCase();
  const dotIndex = normalizedName.lastIndexOf('.');

  return dotIndex < 0 || dotIndex === normalizedName.length - 1
    ? ''
    : normalizedName.slice(dotIndex + 1);
}

export function isCsvFile(
  fileName: string | null | undefined,
  mimeType: string | null | undefined,
): boolean {
  return (mimeType?.toLowerCase().includes('csv') ?? false) || getFileExtension(fileName) === 'csv';
}

export function isCsvDatasetFile(file: Pick<File, 'name' | 'type'>): boolean {
  return isCsvFile(file.name, file.type);
}
