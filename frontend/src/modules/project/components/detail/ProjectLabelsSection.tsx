import { Tags } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { type ProjectDetail } from '@/modules/project/types/project';

type ProjectLabelsSectionProps = Readonly<{
  project: ProjectDetail;
}>;

export function ProjectLabelsSection({ project }: ProjectLabelsSectionProps) {
  const { t } = useTranslation();

  return (
    <section className="rounded-xl border border-border bg-surface-base p-6 sm:p-8">
      <h2 className="mb-4 flex items-center gap-2 text-lg font-bold text-primary">
        <Tags className="size-5" />
        {t('project.detail.labelsTitle')}
      </h2>

      {project.labels.length === 0 ? (
        <p className="text-sm text-muted-foreground">{t('project.detail.noLabels')}</p>
      ) : (
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
          {project.labels.map((label) => (
            <div
              className="flex items-center gap-3 rounded-lg border p-3"
              key={`${label.name}-${label.color ?? 'none'}`}
              style={{
                backgroundColor: label.color ? `${label.color}0A` : undefined,
                borderColor: label.color ? `${label.color}40` : undefined,
              }}
            >
              {label.color && (
                <span
                  className="inline-block size-3 shrink-0 rounded-full"
                  style={{ backgroundColor: label.color }}
                />
              )}
              <span
                className="truncate text-sm font-semibold"
                style={{ color: label.color ?? undefined }}
              >
                {label.name}
              </span>
            </div>
          ))}
        </div>
      )}
    </section>
  );
}
