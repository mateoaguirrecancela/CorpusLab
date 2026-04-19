import { X } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Button } from '@/components/ui/button';
import { UploadDropzone } from '@/modules/project/components/UploadDropzone';

function formatFileSize(bytes: number): string {
  if (bytes < 1024) {
    return `${bytes} B`;
  }
  if (bytes < 1024 * 1024) {
    return `${(bytes / 1024).toFixed(1)} KB`;
  }

  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

type CreateProjectDatasetStepProps = Readonly<{
  selectedFiles: File[];
  canContinue: boolean;
  onBack: () => void;
  onContinue: () => void;
  onFilesSelected: (files: FileList | null) => void;
  onRemoveFile: (fileName: string, index: number) => void;
}>;

export function CreateProjectDatasetStep({
  selectedFiles,
  canContinue,
  onBack,
  onContinue,
  onFilesSelected,
  onRemoveFile,
}: CreateProjectDatasetStepProps) {
  const { t } = useTranslation();

  return (
    <div className="mt-8 space-y-3">
      <p className="text-xs font-bold uppercase tracking-wider text-muted-foreground">
        {t('project.create.steps.dataset')} *
      </p>

      <UploadDropzone
        accept=".pdf,.txt,.json,.csv,image/*"
        description={t('project.create.selectFilesHint')}
        multiple
        onFilesChange={onFilesSelected}
        title={t('project.create.selectFiles')}
      />

      {selectedFiles.length > 0 && (
        <div className="space-y-2 rounded-md border border-border bg-background p-3">
          {selectedFiles.map((file, index) => (
            <div className="flex items-center justify-between gap-3" key={`${file.name}-${index}`}>
              <div className="min-w-0">
                <p className="truncate text-sm font-semibold text-primary">{file.name}</p>
                <p className="text-xs text-muted-foreground">{formatFileSize(file.size)}</p>
              </div>
              <button
                aria-label={t('project.create.removeFile')}
                className="inline-flex size-8 items-center justify-center rounded-md text-muted-foreground transition-colors hover:bg-accent hover:text-primary"
                onClick={() => onRemoveFile(file.name, index)}
                type="button"
              >
                <X className="size-4" />
              </button>
            </div>
          ))}
        </div>
      )}

      <div className="flex justify-end gap-3">
        <Button
          className="h-10 rounded-md border border-border bg-surface-base px-6 text-sm font-semibold text-primary hover:bg-accent cursor-pointer"
          onClick={onBack}
          type="button"
          variant="outline"
        >
          {t('project.create.previousStepSimple')}
        </Button>
        <Button
          className="h-10 min-w-36 rounded-md bg-primary text-sm font-semibold text-white transition-colors hover:bg-primary-strong disabled:bg-secondary cursor-pointer"
          disabled={!canContinue}
          onClick={onContinue}
          type="button"
        >
          {t('project.create.nextStepSimple')}
        </Button>
      </div>
    </div>
  );
}
