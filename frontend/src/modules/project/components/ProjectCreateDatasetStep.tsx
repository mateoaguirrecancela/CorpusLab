import { FileText, X } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Button } from '@/components/ui/button';
import {
  UploadDropzone,
  type UploadDropzoneAccept,
  type UploadDropzoneFileRejection,
} from '@/modules/project/components/UploadDropzone';
import { formatFileSize } from '@/modules/project/utils/projectDisplayUtils';

const DATASET_ACCEPT: UploadDropzoneAccept = {
  'application/json': ['.json'],
  'application/pdf': ['.pdf'],
  'image/*': [],
  'text/csv': ['.csv'],
  'text/plain': ['.txt'],
};

type ProjectCreateDatasetStepProps = Readonly<{
  selectedFiles: File[];
  canContinue: boolean;
  onBack: () => void;
  onContinue: () => void;
  onFilesRejected: (fileRejections: UploadDropzoneFileRejection[]) => void;
  onFilesSelected: (files: File[]) => void;
  onRemoveFile: (fileName: string, index: number) => void;
}>;

export function ProjectCreateDatasetStep({
  selectedFiles,
  canContinue,
  onBack,
  onContinue,
  onFilesRejected,
  onFilesSelected,
  onRemoveFile,
}: ProjectCreateDatasetStepProps) {
  const { t } = useTranslation();

  return (
    <div className="mt-8 space-y-3">
      <p className="text-xs font-bold uppercase tracking-wider text-muted-foreground">
        {t('project.create.steps.dataset')} *
      </p>

      <UploadDropzone
        accept={DATASET_ACCEPT}
        description={t('project.create.selectFilesHint')}
        multiple
        onFilesChange={onFilesSelected}
        onFilesRejected={onFilesRejected}
        title={t('project.create.selectFiles')}
      />

      {selectedFiles.length > 0 && (
        <div className="space-y-3">
          {selectedFiles.map((file, index) => (
            <div
              className="flex items-center justify-between gap-4 rounded-lg border border-border bg-surface-base p-4"
              key={`${file.name}-${file.size}-${index}`}
            >
              <div className="flex min-w-0 items-center gap-3">
                <div className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-danger-soft text-danger-border">
                  <FileText className="size-5 text-red-600" />
                </div>
                <div className="min-w-0">
                  <p className="truncate text-sm font-medium text-foreground">{file.name}</p>
                  <p className="text-xs text-muted-foreground">
                    {file.type || 'application/octet-stream'} - {formatFileSize(file.size)}
                  </p>
                </div>
              </div>
              <button
                aria-label={t('project.create.removeFile')}
                className="inline-flex size-9 shrink-0 items-center justify-center rounded-md text-muted-foreground transition-colors hover:bg-surface-soft hover:text-primary cursor-pointer"
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
