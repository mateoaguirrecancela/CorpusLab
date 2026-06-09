import { useCallback } from 'react';
import { FileUp } from 'lucide-react';
import { useDropzone, type Accept, type FileRejection } from 'react-dropzone';
import { cn } from '@/shared/utils/cn';

export type UploadDropzoneAccept = Accept;
export type UploadDropzoneFileRejection = FileRejection;

type UploadDropzoneProps = Readonly<{
  accept: Accept;
  description: string;
  multiple?: boolean;
  onFilesChange: (files: File[]) => void;
  onFilesRejected?: (fileRejections: FileRejection[]) => void;
  title: string;
}>;

export function UploadDropzone({
  accept,
  description,
  multiple = false,
  onFilesChange,
  onFilesRejected,
  title,
}: UploadDropzoneProps) {
  const handleDrop = useCallback(
    (acceptedFiles: File[], fileRejections: FileRejection[]) => {
      if (acceptedFiles.length > 0) {
        onFilesChange(acceptedFiles);
      }

      if (fileRejections.length > 0) {
        onFilesRejected?.(fileRejections);
      }
    },
    [onFilesChange, onFilesRejected],
  );
  const { getInputProps, getRootProps, isDragAccept, isDragActive, isDragReject, isFocused } =
    useDropzone({
      accept,
      multiple,
      onDrop: handleDrop,
    });

  const rootProps = getRootProps({
    'aria-label': title,
    className: cn(
      'block cursor-pointer rounded-md border border-dashed border-border bg-background p-6 text-center transition-colors',
      isDragAccept ? 'border-primary bg-accent/40' : 'hover:bg-accent/30',
      isDragReject ? 'border-destructive bg-destructive/10' : '',
      isDragActive && !isDragAccept && !isDragReject ? 'border-primary bg-accent/40' : '',
      isFocused ? 'outline-none ring-2 ring-ring/50' : '',
    ),
    role: 'button',
  });
  const inputProps = getInputProps({
    className: 'hidden',
    multiple,
  });

  return (
    <div {...rootProps}>
      <FileUp className="mx-auto size-8 text-primary" />
      <p className="mt-3 text-sm font-semibold text-primary">{title}</p>
      <p className="mt-1 text-xs text-muted-foreground">{description}</p>
      <input {...inputProps} />
    </div>
  );
}
