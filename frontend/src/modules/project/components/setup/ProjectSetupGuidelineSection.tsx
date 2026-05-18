import { FileText, FileUp, X } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Textarea } from '@/components/ui/textarea';
import {
  UploadDropzone,
  type UploadDropzoneFileRejection,
} from '@/modules/project/components/UploadDropzone';
import { formatFileSize } from '@/modules/project/utils/projectDisplayUtils';
import { GUIDELINE_PDF_ACCEPT } from '@/modules/project/utils/projectSetupGuidelineUtils';

type GuidelineMode = 'TEXT' | 'PDF';

type ProjectSetupGuidelineSectionProps = Readonly<{
  guidelineMode: GuidelineMode;
  guidelinePdfErrorMessage: string | undefined;
  guidelinePdfFile: File | null;
  guidelineText: string;
  guidelineTextErrorMessage: string | undefined;
  onGuidelineModeChange: (mode: GuidelineMode) => void;
  onGuidelinePdfRejected: (fileRejections: UploadDropzoneFileRejection[]) => void;
  onGuidelinePdfRemove: () => void;
  onGuidelinePdfSelected: (files: File[]) => void;
  onGuidelineTextChange: (nextText: string) => void;
}>;

function getTabClassName(isSelected: boolean, includesDivider = false): string {
  return [
    'inline-flex items-center justify-center gap-2 px-4 py-3 text-xs font-bold tracking-wider uppercase transition-colors sm:justify-start sm:px-6',
    includesDivider ? 'border-r border-primary/20' : '',
    isSelected
      ? 'bg-surface-base text-primary shadow-[inset_0_-2px_0_0] shadow-primary'
      : 'text-muted-foreground hover:bg-accent/30',
  ].join(' ');
}

export function ProjectSetupGuidelineSection({
  guidelineMode,
  guidelinePdfErrorMessage,
  guidelinePdfFile,
  guidelineText,
  guidelineTextErrorMessage,
  onGuidelineModeChange,
  onGuidelinePdfRejected,
  onGuidelinePdfRemove,
  onGuidelinePdfSelected,
  onGuidelineTextChange,
}: ProjectSetupGuidelineSectionProps) {
  const { t } = useTranslation();
  const isTextMode = guidelineMode === 'TEXT';

  return (
    <section className="space-y-3">
      <p className="text-xs font-bold uppercase tracking-wider text-muted-foreground">
        {t('project.create.guidelineSectionTitle')}
      </p>

      <div className="overflow-hidden rounded-lg border border-primary/20 bg-surface-base">
        <div className="flex border-b border-primary/20 bg-primary/5">
          <button
            className={getTabClassName(isTextMode, true)}
            onClick={() => onGuidelineModeChange('TEXT')}
            type="button"
          >
            <FileText className="size-3.5 text-muted-foreground" />
            {t('project.create.guidelineWriteTab')}
          </button>
          <button
            className={getTabClassName(!isTextMode)}
            onClick={() => onGuidelineModeChange('PDF')}
            type="button"
          >
            <FileUp className="size-3.5 text-muted-foreground" />
            {t('project.create.guidelineUploadTab')}
          </button>
        </div>

        <div className="bg-slate-50/60 p-4 sm:p-6">
          {isTextMode ? (
            <>
              <Textarea
                aria-invalid={Boolean(guidelineTextErrorMessage)}
                id="project-guideline-text"
                maxLength={5000}
                onChange={(event) => onGuidelineTextChange(event.target.value)}
                placeholder={t('project.create.guidelineTextPlaceholder')}
                required
                value={guidelineText}
              />
              {guidelineTextErrorMessage ? (
                <p className="mt-2 text-xs text-destructive">{guidelineTextErrorMessage}</p>
              ) : null}
            </>
          ) : (
            <>
              <UploadDropzone
                accept={GUIDELINE_PDF_ACCEPT}
                description={t('project.create.guidelinePdfHint')}
                onFilesChange={onGuidelinePdfSelected}
                onFilesRejected={onGuidelinePdfRejected}
                title={t('project.create.guidelineUploadTitle')}
              />

              {guidelinePdfErrorMessage ? (
                <p className="mt-2 text-xs text-destructive">{guidelinePdfErrorMessage}</p>
              ) : null}

              {guidelinePdfFile && (
                <div className="mt-4 flex items-center justify-between gap-4 rounded-lg border border-border bg-surface-base p-4">
                  <div className="flex min-w-0 items-center gap-3">
                    <div className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-danger-soft text-danger-border">
                      <FileText className="size-5 text-red-600" />
                    </div>
                    <div className="min-w-0">
                      <p className="truncate text-sm font-medium text-foreground">
                        {guidelinePdfFile.name}
                      </p>
                      <p className="text-xs text-muted-foreground">
                        {guidelinePdfFile.type || 'application/pdf'} -{' '}
                        {formatFileSize(guidelinePdfFile.size)}
                      </p>
                    </div>
                  </div>
                  <button
                    aria-label={t('project.create.removeFile')}
                    className="inline-flex size-9 shrink-0 items-center justify-center rounded-md text-muted-foreground transition-colors hover:bg-surface-soft hover:text-primary cursor-pointer"
                    onClick={onGuidelinePdfRemove}
                    type="button"
                  >
                    <X className="size-4" />
                  </button>
                </div>
              )}
            </>
          )}
        </div>
      </div>
    </section>
  );
}
