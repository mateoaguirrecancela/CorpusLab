import { Database, Download, ExternalLink, FileText } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Button } from '@/components/ui/button';
import { type ProjectDetail } from '@/modules/project/types/project';
import { formatFileSize, isCsvFile } from '@/modules/project/utils/projectDisplayUtils';

type ProjectDatasetSectionProps = Readonly<{
  project: ProjectDetail;
  onDownloadDatasetFile: (datasetItemId: number, fallbackFileName: string) => void;
  onOpenDatasetFile: (datasetItemId: number) => void;
}>;

export function ProjectDatasetSection({
  project,
  onDownloadDatasetFile,
  onOpenDatasetFile,
}: ProjectDatasetSectionProps) {
  const { t } = useTranslation();

  return (
    <section className="rounded-xl border border-border bg-surface-base p-6 sm:p-8">
      <h2 className="mb-4 flex items-center gap-2 text-lg font-bold text-primary">
        <Database className="size-5" />
        {t('project.detail.datasetTitle')}
      </h2>

      {project.datasetItems.length === 0 ? (
        <p className="text-sm text-muted-foreground">{t('project.detail.datasetEmpty')}</p>
      ) : (
        <div className="space-y-3">
          {project.datasetItems.map((item) => (
            <div
              className="flex items-center justify-between gap-4 rounded-lg border border-border bg-surface-base p-4"
              key={item.id}
            >
              <div className="flex min-w-0 items-center gap-3">
                <div className="flex size-10 items-center justify-center rounded-lg bg-danger-soft text-danger-border">
                  <FileText className="size-5 text-red-600" />
                </div>
                <div className="min-w-0">
                  <p className="truncate text-sm font-medium text-foreground">{item.fileName}</p>
                  <p className="text-xs text-muted-foreground">
                    {item.mimeType || 'application/octet-stream'} - {formatFileSize(item.sizeBytes)}
                  </p>
                </div>
              </div>
              <div className="flex shrink-0 items-center gap-2">
                <Button
                  className="h-9 gap-1.5 rounded-md border border-border bg-transparent px-3 text-xs font-semibold text-foreground hover:bg-surface-soft hover:text-primary cursor-pointer"
                  onClick={() => onDownloadDatasetFile(item.id, item.fileName)}
                  type="button"
                >
                  <Download className="size-3.5" />
                  {t('project.detail.downloadDatasetFile')}
                </Button>
                {!isCsvFile(item.mimeType || '', item.fileName) && (
                  <Button
                    className="h-9 gap-1.5 rounded-md border border-border bg-transparent px-3 text-xs font-semibold text-foreground hover:bg-surface-soft hover:text-primary cursor-pointer"
                    onClick={() => onOpenDatasetFile(item.id)}
                    type="button"
                  >
                    <ExternalLink className="size-3.5" />
                    {t('project.detail.openDatasetFile')}
                  </Button>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </section>
  );
}
