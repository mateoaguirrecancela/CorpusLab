import { FileText, X } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Button } from '@/components/ui/button';
import {
  UploadDropzone,
  type UploadDropzoneAccept,
  type UploadDropzoneFileRejection,
} from '@/modules/project/shared/components/UploadDropzone';
import { formatFileSize } from '@/modules/project/shared/utils/projectDisplayUtils';

const DATASET_ACCEPT: UploadDropzoneAccept = {
  'text/plain': ['.txt'],
  'application/json': ['.json'],
  'text/csv': ['.csv'],
  'application/pdf': ['.pdf'],
  'image/jpeg': ['.jpg', '.jpeg'],
  'image/png': ['.png'],
  'image/webp': ['.webp'],
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
        <Button onClick={onBack} size="action" type="button" variant="secondaryAction">
          {t('project.create.previousStepSimple')}
        </Button>
        <Button
          disabled={!canContinue}
          onClick={onContinue}
          size="action-wide"
          type="button"
          variant="primaryAction"
        >
          {t('project.create.nextStepSimple')}
        </Button>
      </div>
    </div>
  );
}
