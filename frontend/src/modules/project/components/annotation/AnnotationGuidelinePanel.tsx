import { ChevronDown, ChevronUp } from 'lucide-react';
import { type ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from '@/components/ui/button';

type AnnotationGuidelinePanelProps = Readonly<{
  children: ReactNode;
  isCollapsed: boolean;
  onToggleCollapsed: () => void;
}>;

export function AnnotationGuidelinePanel({
  children,
  isCollapsed,
  onToggleCollapsed,
}: AnnotationGuidelinePanelProps) {
  const { t } = useTranslation();

  return (
    <section className="rounded-xl border border-border bg-surface-base p-5 sm:p-6">
      <div className="flex items-center justify-between gap-3">
        <h2 className="text-sm font-bold tracking-[0.12em] text-muted-foreground uppercase">
          {t('project.annotationPage.guidelineTitle')}
        </h2>
        <Button
          aria-controls="annotation-guideline-content"
          aria-expanded={!isCollapsed}
          className="h-8 gap-1 px-2 text-xs"
          onClick={onToggleCollapsed}
          type="button"
          variant="ghost"
        >
          {isCollapsed ? (
            <ChevronDown aria-hidden className="size-4" />
          ) : (
            <ChevronUp aria-hidden className="size-4" />
          )}
        </Button>
      </div>

      {!isCollapsed && (
        <div className="mt-3 border-t pt-3 bg-surface-soft" id="annotation-guideline-content">
          {children}
        </div>
      )}
    </section>
  );
}
