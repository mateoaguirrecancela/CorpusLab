import { FileUp } from 'lucide-react';

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
  return (
    <label className="block cursor-pointer rounded-md border border-dashed border-border bg-background p-6 text-center hover:bg-accent/30">
      <FileUp className="mx-auto size-8 text-primary" />
      <p className="mt-3 text-sm font-semibold text-primary">{title}</p>
      <p className="mt-1 text-xs text-muted-foreground">{description}</p>
      <input
        accept={accept}
        className="hidden"
        multiple={multiple}
        onChange={(event) => onFilesChange(event.target.files)}
        type="file"
      />
    </label>
  );
}
