import { type DragEvent, useRef, useState } from 'react';
import { FileUp } from 'lucide-react';
import { cn } from '@/lib/utils';

type UploadDropzoneProps = Readonly<{
  accept: string;
  description: string;
  multiple?: boolean;
  onFilesChange: (files: FileList | null) => void;
  title: string;
}>;

export function UploadDropzone({
  accept,
  description,
  multiple = false,
  onFilesChange,
  title,
}: UploadDropzoneProps) {
  const [isDragActive, setIsDragActive] = useState(false);
  const dragDepth = useRef(0);

  const handleDragEnter = (event: DragEvent<HTMLLabelElement>) => {
    event.preventDefault();
    event.stopPropagation();

    dragDepth.current += 1;
    setIsDragActive(true);
  };

  const handleDragOver = (event: DragEvent<HTMLLabelElement>) => {
    event.preventDefault();
    event.stopPropagation();
    event.dataTransfer.dropEffect = 'copy';
  };

  const handleDragLeave = (event: DragEvent<HTMLLabelElement>) => {
    event.preventDefault();
    event.stopPropagation();

    dragDepth.current = Math.max(0, dragDepth.current - 1);
    if (dragDepth.current === 0) {
      setIsDragActive(false);
    }
  };

  const handleDrop = (event: DragEvent<HTMLLabelElement>) => {
    event.preventDefault();
    event.stopPropagation();

    dragDepth.current = 0;
    setIsDragActive(false);

    const droppedFiles = event.dataTransfer.files;
    onFilesChange(droppedFiles.length > 0 ? droppedFiles : null);
  };

  return (
    <label
      className={cn(
        'block cursor-pointer rounded-md border border-dashed border-border bg-background p-6 text-center transition-colors',
        isDragActive ? 'border-primary bg-accent/40' : 'hover:bg-accent/30',
      )}
      onDragEnter={handleDragEnter}
      onDragLeave={handleDragLeave}
      onDragOver={handleDragOver}
      onDrop={handleDrop}
    >
      <FileUp className="mx-auto size-8 text-primary" />
      <p className="mt-3 text-sm font-semibold text-primary">{title}</p>
      <p className="mt-1 text-xs text-muted-foreground">{description}</p>
      <input
        accept={accept}
        className="hidden"
        multiple={multiple}
        onChange={(event) =>
          onFilesChange(
            event.target.files && event.target.files.length > 0 ? event.target.files : null,
          )
        }
        type="file"
      />
    </label>
  );
}
