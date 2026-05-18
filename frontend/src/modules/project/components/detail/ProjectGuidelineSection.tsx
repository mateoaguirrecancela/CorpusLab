import { Download, ExternalLink, FileText } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Button } from '@/components/ui/button';
import { type ProjectDetail } from '@/modules/project/types/project';

type ProjectGuidelineSectionProps = Readonly<{
  guidelinePdfMetadata: string;
  project: ProjectDetail;
  onDownloadGuidelinePdf: () => Promise<void>;
  onOpenGuidelinePdf: () => Promise<void>;
}>;

export function ProjectGuidelineSection({
  guidelinePdfMetadata,
  project,
  onDownloadGuidelinePdf,
  onOpenGuidelinePdf,
}: ProjectGuidelineSectionProps) {
  const { t } = useTranslation();

  return (
    <section className="rounded-xl border border-border bg-surface-base p-6 sm:p-8">
      <h2 className="mb-4 flex items-center gap-2 text-lg font-bold text-primary">
        <FileText className="size-5" />
        {t('project.detail.guidelineTitle')}
      </h2>

      {project.guidelineText && (
        <div className="rounded-lg border border-border bg-surface-base p-5">
          <p className="whitespace-pre-wrap text-sm leading-relaxed text-muted-foreground">
            {project.guidelineText}
          </p>
        </div>
      )}

      {!project.guidelineText && project.guidelinePdfAvailable && (
        <div className="flex items-center justify-between gap-4 rounded-lg border border-border bg-surface-base p-4">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-lg bg-danger-soft text-danger-border">
              <FileText className="size-5 text-red-600" />
            </div>
            <div>
              <p className="text-sm font-medium text-foreground">
                {t('project.detail.guidelineDocument')}
              </p>
              <p className="text-xs text-muted-foreground">
                {guidelinePdfMetadata || t('project.detail.guidelinePdfFile')}
              </p>
            </div>
          </div>
          <div className="flex shrink-0 items-center gap-2">
            <Button
              className="h-9 gap-1.5 rounded-md border border-border bg-transparent px-3 text-xs font-semibold text-foreground hover:bg-surface-soft hover:text-primary cursor-pointer"
              onClick={() => void onDownloadGuidelinePdf()}
              type="button"
            >
              <Download className="size-3.5" />
              {t('project.detail.downloadDatasetFile')}
            </Button>
            <Button
              className="h-9 gap-1.5 rounded-md border border-border bg-transparent px-3 text-xs font-semibold text-foreground hover:bg-surface-soft hover:text-primary cursor-pointer"
              onClick={() => void onOpenGuidelinePdf()}
              type="button"
            >
              <ExternalLink className="size-3.5" />
              {t('project.detail.openDatasetFile')}
            </Button>
          </div>
        </div>
      )}

      {!project.guidelineText && !project.guidelinePdfAvailable && (
        <p className="text-sm text-muted-foreground">{t('project.detail.noGuideline')}</p>
      )}
    </section>
  );
}
