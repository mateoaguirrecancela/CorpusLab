import { useTranslation } from 'react-i18next';
import { type ResearchGroupDetail } from '@/modules/researchgroup/types/researchGroup';

type ResearchGroupOverviewSectionProps = Readonly<{
  group: ResearchGroupDetail;
}>;

export function ResearchGroupOverviewSection({ group }: ResearchGroupOverviewSectionProps) {
  const { t } = useTranslation();

  return (
    <section className="rounded-md border border-border bg-surface-base p-6 sm:p-7">
      <div className="flex flex-col gap-5 md:flex-row md:items-start md:justify-between">
        <div>
          <h1 className="text-4xl font-black tracking-tight text-primary">{group.name}</h1>
          {group.description && (
            <p className="mt-2 max-w-3xl text-lg leading-tight text-muted-foreground">
              {group.description}
            </p>
          )}
        </div>

        <div className="flex lg:justify-end gap-4">
          <div className="rounded-md bg-background p-4 text-center w-30 h-30">
            <p className="text-xs font-bold tracking-widest text-muted-foreground uppercase">
              {t('researchGroup.detail.activeProjects')}
            </p>
            <p className="mt-2 text-4xl font-black leading-none text-primary">
              {group.activeProjects}
            </p>
          </div>

          <div className="rounded-md bg-background p-4 text-center w-30 h-30">
            <p className="text-xs font-bold tracking-widest text-muted-foreground uppercase">
              {t('researchGroup.detail.totalMembers')}
            </p>
            <p className="mt-2 text-4xl font-black leading-none text-primary">
              {group.totalMembers}
            </p>
          </div>
        </div>
      </div>
    </section>
  );
}
